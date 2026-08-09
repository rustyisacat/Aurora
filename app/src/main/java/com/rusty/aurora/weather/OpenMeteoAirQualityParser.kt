package com.rusty.aurora.weather

import kotlinx.serialization.json.Json

/** Pure JSON -> AQI Int? conversion, kept separate from
 *  [OpenMeteoAirQualityClient] for the same reason as every other
 *  client/parser split in this package - testable without a real network
 *  call. */
internal object OpenMeteoAirQualityParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): Int? =
        json.decodeFromString(OpenMeteoAirQualityResponse.serializer(), rawJson).current.usAqi
}
