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
            rainExpectedAt = findRainExpectedAt(response)
        )
    }

    /** "2026-07-27T06:15" -> "06:15" */
    private fun extractTimeOfDay(isoDateTime: String): String? =
        isoDateTime.substringAfter('T', missingDelimiterValue = "").ifEmpty { null }

    /** First hour from now through the end of today whose precipitation
     *  probability clears [RAIN_PROBABILITY_THRESHOLD] - null if none does.
     *  ISO 8601 datetime strings sort chronologically as plain strings, so
     *  this needs no date parsing to compare against [current time][isoNow]. */
    private fun findRainExpectedAt(response: OpenMeteoResponse): String? {
        val isoNow = response.current.time
        val hourly = response.hourly
        for (i in hourly.time.indices) {
            val hour = hourly.time[i]
            if (hour < isoNow) continue
            val probability = hourly.precipitationProbability.getOrNull(i) ?: continue
            if (probability >= RAIN_PROBABILITY_THRESHOLD) return extractTimeOfDay(hour)
        }
        return null
    }
}
