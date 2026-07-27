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
    val timezone: String
) {
    @Serializable
    data class CurrentBlock(
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val weatherCode: Int
    )

    @Serializable
    data class DailyBlock(
        @SerialName("temperature_2m_max") val high: List<Double>,
        @SerialName("temperature_2m_min") val low: List<Double>,
        // ISO 8601 local datetimes (e.g. "2026-07-27T06:15") - already in
        // whatever timezone this response's own `timezone` field names,
        // thanks to &timezone=auto, so no conversion is needed here.
        val sunrise: List<String> = emptyList(),
        val sunset: List<String> = emptyList()
    )
}
