package com.rusty.aurora.weather

/**
 * Live + cached severe weather alerts for Aurora's fixed home location (see
 * [WeatherConfig]) - same never-blocks-on-network contract as
 * [WeatherRepository.getWeather].
 *
 * [getAlert] returns null both when there's genuinely no active alert and
 * when nothing has been fetched yet - unlike weather, "no data" and "clear
 * skies, no alert" are indistinguishable and that's fine: either way there's
 * nothing to show on the dashboard.
 */
interface WeatherAlertRepository {
    fun getAlert(): WeatherAlert?
}
