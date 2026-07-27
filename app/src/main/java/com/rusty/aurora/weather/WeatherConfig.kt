package com.rusty.aurora.weather

/**
 * Fallback coordinate, used only when LocationRepository can't provide one
 * (location permission not granted, or no fix has ever been obtained - see
 * WeatherRepositoryImpl). Weather otherwise follows the phone's actual
 * location automatically.
 *
 * Currently set to Jacksonville, FL 32258 (approximate). Update these two
 * constants if this fallback should point somewhere else.
 */
object WeatherConfig {
    const val LATITUDE = 30.1588
    const val LONGITUDE = -81.6206
    const val CACHE_DURATION_MILLIS = 15 * 60 * 1000L
}
