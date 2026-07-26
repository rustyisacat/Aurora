package com.rusty.aurora.weather

/**
 * Aurora is a bedside dashboard bolted to one physical location, not a
 * mobile weather app - a fixed coordinate is simpler and more reliable
 * than adding a location permission and taking a GPS fix on every refresh.
 *
 * Currently set to Jacksonville, FL 32258 (approximate). Update these two
 * constants if Aurora's home location ever changes.
 */
object WeatherConfig {
    const val LATITUDE = 30.1588
    const val LONGITUDE = -81.6206
    const val CACHE_DURATION_MILLIS = 15 * 60 * 1000L
}
