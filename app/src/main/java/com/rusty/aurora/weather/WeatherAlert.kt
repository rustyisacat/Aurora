package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

/** The single highest-severity active NWS alert for Aurora's location, if any. */
@Serializable
data class WeatherAlert(
    val event: String,
    val headline: String,
    // NWS's own severity string ("Extreme", "Severe", "Moderate", "Minor",
    // "Unknown") - passed through as-is rather than mapped to an enum, same
    // reasoning as WeatherSnapshot.condition being a plain String.
    val severity: String
)
