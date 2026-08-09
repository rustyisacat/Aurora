package com.rusty.aurora.weather

import kotlinx.serialization.json.Json

/** Pure JSON -> radar station id conversion, kept separate from
 *  [NwsPointsClient] for the same reason as OpenMeteoResponseParser/
 *  NwsAlertParser - testable without a real network call. */
internal object NwsPointsParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): String? =
        json.decodeFromString(NwsPointsResponse.serializer(), rawJson).properties.radarStation
}
