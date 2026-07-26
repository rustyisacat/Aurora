package com.rusty.aurora.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape of the subset of Open-Meteo's forecast response Aurora actually reads. */
@Serializable
internal data class OpenMeteoResponse(
    val current: CurrentBlock,
    val daily: DailyBlock
) {
    @Serializable
    data class CurrentBlock(
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val weatherCode: Int
    )

    @Serializable
    data class DailyBlock(
        @SerialName("temperature_2m_max") val high: List<Double>,
        @SerialName("temperature_2m_min") val low: List<Double>
    )
}
