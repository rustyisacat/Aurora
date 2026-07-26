package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionMapperTest {

    @Test
    fun `maps known WMO codes to expected labels`() {
        assertEquals("Clear", WeatherConditionMapper.toCondition(0))
        assertEquals("Partly Cloudy", WeatherConditionMapper.toCondition(2))
        assertEquals("Overcast", WeatherConditionMapper.toCondition(3))
        assertEquals("Fog", WeatherConditionMapper.toCondition(45))
        assertEquals("Rain", WeatherConditionMapper.toCondition(63))
        assertEquals("Snow", WeatherConditionMapper.toCondition(75))
        assertEquals("Thunderstorm", WeatherConditionMapper.toCondition(95))
    }

    @Test
    fun `unrecognized code maps to Unknown rather than throwing`() {
        assertEquals("Unknown", WeatherConditionMapper.toCondition(-1))
    }
}
