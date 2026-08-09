package com.rusty.aurora.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape of the subset of Open-Meteo's air quality response Aurora
 *  actually reads - a separate API (air-quality-api.open-meteo.com) from
 *  the main forecast one, same provider and unauthenticated-GET style. */
@Serializable
internal data class OpenMeteoAirQualityResponse(
    val current: CurrentBlock
) {
    @Serializable
    data class CurrentBlock(
        // US EPA index (0-500+) - requested via &current=us_aqi. Null if
        // Open-Meteo couldn't compute one for this coordinate (e.g. no
        // nearby monitoring/model coverage), not just if the key is absent.
        @SerialName("us_aqi") val usAqi: Int? = null
    )
}
