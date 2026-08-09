package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

/** Shape of the subset of the NWS `/alerts/active` GeoJSON response Aurora
 *  actually reads - it's a FeatureCollection, one Feature per active alert. */
@Serializable
internal data class NwsAlertsResponse(
    val features: List<Feature> = emptyList()
) {
    @Serializable
    data class Feature(
        val properties: Properties
    )

    @Serializable
    data class Properties(
        val event: String,
        // Occasionally blank on lower-severity products - falls back to
        // `event` in NwsAlertParser rather than leaving the dashboard banner
        // with an empty second line.
        val headline: String? = null,
        val severity: String = "Unknown"
    )
}
