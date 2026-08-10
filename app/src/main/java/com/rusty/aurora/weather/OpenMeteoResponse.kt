package com.rusty.aurora.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape of the subset of Open-Meteo's forecast response Aurora actually reads. */
@Serializable
internal data class OpenMeteoResponse(
    val current: CurrentBlock,
    val daily: DailyBlock,
    // Present because the request includes "&timezone=auto" - an IANA name
    // (e.g. "America/New_York") resolved from the request's lat/long, which
    // is what lets the dashboard's clock follow wherever the phone actually is.
    val timezone: String,
    // Defaults to empty rather than required, same reasoning as
    // DailyBlock's sunrise/sunset below - degrades to "no rain forecast
    // available" rather than failing the whole parse if it's ever missing.
    val hourly: HourlyBlock = HourlyBlock()
) {
    @Serializable
    data class CurrentBlock(
        // Open-Meteo always includes this regardless of what's in &current=
        // - needed to know which hourly.time entries are still ahead of us
        // today, not already past. Defaults to empty (never true-empty in a
        // real response) so old-shaped test fixtures without it still parse.
        val time: String = "",
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val weatherCode: Int,
        @SerialName("apparent_temperature") val feelsLike: Double? = null,
        @SerialName("wind_speed_10m") val windSpeed: Double? = null,
        @SerialName("relative_humidity_2m") val humidity: Int? = null,
        @SerialName("uv_index") val uvIndex: Double? = null
    )

    @Serializable
    data class DailyBlock(
        @SerialName("temperature_2m_max") val high: List<Double>,
        @SerialName("temperature_2m_min") val low: List<Double>,
        // ISO 8601 local datetimes (e.g. "2026-07-27T06:15") - already in
        // whatever timezone this response's own `timezone` field names,
        // thanks to &timezone=auto, so no conversion is needed here.
        val sunrise: List<String> = emptyList(),
        val sunset: List<String> = emptyList(),
        // Plain "YYYY-MM-DD", one per forecast day (index 0 is today) -
        // used to build DailyForecastEntry.date, and to line up with
        // high/low/weatherCode by index.
        val time: List<String> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Int> = emptyList()
    )

    @Serializable
    data class HourlyBlock(
        // ISO 8601 local datetimes, one per hour of forecast_days - same
        // "already in the right timezone" note as DailyBlock's sunrise/sunset.
        val time: List<String> = emptyList(),
        @SerialName("precipitation_probability") val precipitationProbability: List<Int> = emptyList()
    )
}
