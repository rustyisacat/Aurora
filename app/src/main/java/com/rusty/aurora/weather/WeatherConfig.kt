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
    // Shorter than weather's own cache - a severe alert is time-critical
    // enough to be worth refreshing more often.
    const val ALERT_CACHE_DURATION_MILLIS = 10 * 60 * 1000L
    // Today plus this many upcoming days, for the dashboard's forecast
    // strip. 5 is a common weather-app convention and fits the Echo
    // Show's width without the per-day cells getting cramped.
    const val FORECAST_DAYS = 5
}
