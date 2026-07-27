package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshot(
    val temperature: Int,
    val condition: String,
    val high: Int,
    val low: Int,
    // IANA name (e.g. "America/New_York") for whatever coordinate this
    // snapshot was fetched for - the dashboard's clock uses this so it
    // always matches wherever the phone actually is, not just whatever
    // system timezone the display device happens to be set to.
    val timezone: String
)
