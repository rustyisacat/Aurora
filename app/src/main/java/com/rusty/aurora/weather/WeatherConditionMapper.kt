package com.rusty.aurora.weather

/** Maps Open-Meteo's WMO weather codes to short human-readable labels. */
internal object WeatherConditionMapper {

    fun toCondition(wmoCode: Int): String = when (wmoCode) {
        0 -> "Clear"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }
}
