package com.rusty.aurora.weather

import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * Pure JSON -> WeatherSnapshot conversion, kept separate from [OpenMeteoClient]
 * so malformed/unexpected response shapes can be exercised in a unit test
 * without any real network call.
 */
internal object OpenMeteoResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    // Below this, "might rain" isn't worth an umbrella nudge - Open-Meteo's
    // hourly probabilities are frequently in the 10-30% range for a mostly
    // dry day, and a warning that fires half the time trains you to ignore it.
    private const val RAIN_PROBABILITY_THRESHOLD = 50

    fun parse(rawJson: String): WeatherSnapshot {
        val response = json.decodeFromString(OpenMeteoResponse.serializer(), rawJson)
        return WeatherSnapshot(
            temperature = response.current.temperature.roundToInt(),
            condition = WeatherConditionMapper.toCondition(response.current.weatherCode),
            high = response.daily.high.first().roundToInt(),
            low = response.daily.low.first().roundToInt(),
            timezone = response.timezone,
            sunrise = response.daily.sunrise.firstOrNull()?.let(::extractTimeOfDay),
            sunset = response.daily.sunset.firstOrNull()?.let(::extractTimeOfDay),
            rainExpectedAt = findRainExpectedAt(response),
            feelsLike = response.current.feelsLike?.roundToInt(),
            windSpeedMph = response.current.windSpeed?.roundToInt(),
            humidityPercent = response.current.humidity,
            uvIndex = response.current.uvIndex?.roundToInt(),
            dailyForecast = buildDailyForecast(response.daily)
        )
    }

    /** Today plus whatever additional days the response's daily block
     *  includes (see WeatherConfig.FORECAST_DAYS) - zips time/high/low/
     *  weatherCode by index, stopping at the shortest of the four in case
     *  Open-Meteo ever returns mismatched lengths. */
    private fun buildDailyForecast(daily: OpenMeteoResponse.DailyBlock): List<DailyForecastEntry> {
        val count = minOf(daily.time.size, daily.high.size, daily.low.size, daily.weatherCode.size)
        return (0 until count).map { i ->
            DailyForecastEntry(
                date = daily.time[i],
                high = daily.high[i].roundToInt(),
                low = daily.low[i].roundToInt(),
                condition = WeatherConditionMapper.toCondition(daily.weatherCode[i])
            )
        }
    }

    /** "2026-07-27T06:15" -> "06:15" */
    private fun extractTimeOfDay(isoDateTime: String): String? =
        isoDateTime.substringAfter('T', missingDelimiterValue = "").ifEmpty { null }

    /** First hour from now through the end of today whose precipitation
     *  probability clears [RAIN_PROBABILITY_THRESHOLD] - null if none does.
     *  ISO 8601 datetime strings sort chronologically as plain strings, so
     *  this needs no date parsing to compare against [current time][isoNow].
     *  Explicitly bounded to today's date prefix - now that the request
     *  covers several forecast days (see WeatherConfig.FORECAST_DAYS), the
     *  hourly array spans more than just today, and this is meant to stay
     *  a same-day "bring an umbrella" nudge, not a several-days-out one. */
    private fun findRainExpectedAt(response: OpenMeteoResponse): String? {
        val isoNow = response.current.time
        val today = isoNow.substringBefore('T')
        val hourly = response.hourly
        for (i in hourly.time.indices) {
            val hour = hourly.time[i]
            if (!hour.startsWith(today)) continue
            if (hour < isoNow) continue
            val probability = hourly.precipitationProbability.getOrNull(i) ?: continue
            if (probability >= RAIN_PROBABILITY_THRESHOLD) return extractTimeOfDay(hour)
        }
        return null
    }
}
