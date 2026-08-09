package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

/** One day of Open-Meteo's daily forecast block, including today itself
 *  (index 0) - the dashboard labels that entry "Today" and derives
 *  weekday labels for the rest client-side from [date], same "just
 *  enough" split as sunrise/sunset already being plain "HH:mm" strings. */
@Serializable
data class DailyForecastEntry(
    // "YYYY-MM-DD", already in whatever timezone the snapshot itself is.
    val date: String,
    val high: Int,
    val low: Int,
    val condition: String
)
