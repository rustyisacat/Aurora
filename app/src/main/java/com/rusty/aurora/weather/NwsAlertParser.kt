package com.rusty.aurora.weather

import kotlinx.serialization.json.Json

/**
 * Pure JSON -> WeatherAlert? conversion, kept separate from [NwsAlertClient]
 * so malformed/unexpected response shapes can be exercised in a unit test
 * without any real network call - same split as OpenMeteoClient/
 * OpenMeteoResponseParser.
 */
internal object NwsAlertParser {

    private val json = Json { ignoreUnknownKeys = true }

    // Lower rank = more severe. Anything NWS reports that isn't one of
    // these four known values (including "Unknown" itself) sorts last.
    private val SEVERITY_RANK = mapOf(
        "Extreme" to 0,
        "Severe" to 1,
        "Moderate" to 2,
        "Minor" to 3
    )

    fun parse(rawJson: String): WeatherAlert? {
        val response = json.decodeFromString(NwsAlertsResponse.serializer(), rawJson)
        val highestSeverity = response.features.minByOrNull { rankOf(it.properties.severity) }
            ?: return null

        val properties = highestSeverity.properties
        return WeatherAlert(
            event = properties.event,
            headline = properties.headline?.ifBlank { null } ?: properties.event,
            severity = properties.severity
        )
    }

    private fun rankOf(severity: String): Int = SEVERITY_RANK[severity] ?: 4
}
