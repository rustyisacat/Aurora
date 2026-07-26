package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshot(
    val temperature: Int,
    val condition: String,
    val high: Int,
    val low: Int
)
