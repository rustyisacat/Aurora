package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoResponseParserTest {

    @Test
    fun `parses temperature, condition, high, low, timezone, sunrise, and sunset from a well-formed response`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "temperature_2m": 74.3, "weather_code": 0 },
              "daily": {
                "temperature_2m_max": [86.1],
                "temperature_2m_min": [68.4],
                "sunrise": ["2026-07-27T06:15"],
                "sunset": ["2026-07-27T20:42"]
              }
            }
        """.trimIndent()

        val result = OpenMeteoResponseParser.parse(json)

        assertEquals(
            WeatherSnapshot(
                temperature = 74,
                condition = "Clear",
                high = 86,
                low = 68,
                timezone = "America/New_York",
                sunrise = "06:15",
                sunset = "20:42"
            ),
            result
        )
    }

    @Test
    fun `sunrise and sunset are null when the response omits them`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "temperature_2m": 74.3, "weather_code": 0 },
              "daily": {
                "temperature_2m_max": [86.1],
                "temperature_2m_min": [68.4]
              }
            }
        """.trimIndent()

        val result = OpenMeteoResponseParser.parse(json)

        assertEquals(null, result.sunrise)
        assertEquals(null, result.sunset)
    }

    @Test
    fun `ignores unknown fields rather than failing to parse`() {
        val json = """
            {
              "latitude": 30.16,
              "longitude": -81.62,
              "timezone": "America/New_York",
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

    @Test
    fun `rainExpectedAt is null when the response has no hourly block`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "temperature_2m": 74.3, "weather_code": 0 },
              "daily": { "temperature_2m_max": [86.1], "temperature_2m_min": [68.4] }
            }
        """.trimIndent()

        assertEquals(null, OpenMeteoResponseParser.parse(json).rainExpectedAt)
    }

    @Test
    fun `rainExpectedAt finds the first upcoming hour clearing the probability threshold`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "time": "2026-07-29T12:00", "temperature_2m": 74.3, "weather_code": 0 },
              "daily": { "temperature_2m_max": [86.1], "temperature_2m_min": [68.4] },
              "hourly": {
                "time": ["2026-07-29T11:00", "2026-07-29T12:00", "2026-07-29T13:00", "2026-07-29T14:00"],
                "precipitation_probability": [80, 20, 30, 60]
              }
            }
        """.trimIndent()

        assertEquals("14:00", OpenMeteoResponseParser.parse(json).rainExpectedAt)
    }

    @Test
    fun `rainExpectedAt ignores hours before now even if they cleared the threshold`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "time": "2026-07-29T12:00", "temperature_2m": 74.3, "weather_code": 0 },
              "daily": { "temperature_2m_max": [86.1], "temperature_2m_min": [68.4] },
              "hourly": {
                "time": ["2026-07-29T11:00", "2026-07-29T12:00"],
                "precipitation_probability": [90, 10]
              }
            }
        """.trimIndent()

        assertEquals(null, OpenMeteoResponseParser.parse(json).rainExpectedAt)
    }

    @Test
    fun `rainExpectedAt is null when no upcoming hour clears the threshold`() {
        val json = """
            {
              "timezone": "America/New_York",
              "current": { "time": "2026-07-29T12:00", "temperature_2m": 74.3, "weather_code": 0 },
              "daily": { "temperature_2m_max": [86.1], "temperature_2m_min": [68.4] },
              "hourly": {
                "time": ["2026-07-29T12:00", "2026-07-29T13:00"],
                "precipitation_probability": [20, 30]
              }
            }
        """.trimIndent()

        assertEquals(null, OpenMeteoResponseParser.parse(json).rainExpectedAt)
    }
}
