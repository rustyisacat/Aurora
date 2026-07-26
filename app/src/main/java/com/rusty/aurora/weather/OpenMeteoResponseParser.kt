package com.rusty.aurora.weather

import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * Pure JSON -> WeatherSnapshot conversion, kept separate from [OpenMeteoClient]
 * so malformed/unexpected response shapes can be exercised in a unit test
 * without any real network call.
 */
internal object OpenMeteoResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): WeatherSnapshot {
        val response = json.decodeFromString(OpenMeteoResponse.serializer(), rawJson)
        return WeatherSnapshot(
            temperature = response.current.temperature.roundToInt(),
            condition = WeatherConditionMapper.toCondition(response.current.weatherCode),
            high = response.daily.high.first().roundToInt(),
            low = response.daily.low.first().roundToInt()
        )
    }
}
