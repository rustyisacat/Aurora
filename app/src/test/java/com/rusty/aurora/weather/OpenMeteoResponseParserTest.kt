package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoResponseParserTest {

    @Test
    fun `parses temperature, condition, high, and low from a well-formed response`() {
        val json = """
            {
              "current": { "temperature_2m": 74.3, "weather_code": 0 },
              "daily": {
                "temperature_2m_max": [86.1],
                "temperature_2m_min": [68.4]
              }
            }
        """.trimIndent()

        val result = OpenMeteoResponseParser.parse(json)

        assertEquals(WeatherSnapshot(temperature = 74, condition = "Clear", high = 86, low = 68), result)
    }

    @Test
    fun `ignores unknown fields rather than failing to parse`() {
        val json = """
            {
              "latitude": 30.16,
              "longitude": -81.62,
              "current": { "temperature_2m": 50.0, "weather_code": 61, "relative_humidity_2m": 80 },
              "daily": {
                "temperature_2m_max": [55.0],
                "temperature_2m_min": [45.0],
                "sunrise": ["2026-07-26T06:00"]
              }
            }
        """.trimIndent()

        val result = OpenMeteoResponseParser.parse(json)

        assertEquals("Rain", result.condition)
    }

    @Test(expected = Exception::class)
    fun `malformed json throws rather than silently returning bad data`() {
        OpenMeteoResponseParser.parse("not json")
    }
}
