package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenMeteoAirQualityParserTest {

    @Test
    fun `parses us_aqi from a well-formed response`() {
        val json = """{ "current": { "us_aqi": 42 } }"""

        assertEquals(42, OpenMeteoAirQualityParser.parse(json))
    }

    @Test
    fun `null when the response omits us_aqi`() {
        val json = """{ "current": {} }"""

        assertNull(OpenMeteoAirQualityParser.parse(json))
    }

    @Test
    fun `ignores unknown fields rather than failing to parse`() {
        val json = """
            {
              "latitude": 30.16,
              "longitude": -81.62,
              "current": { "time": "2026-07-29T12:00", "pm10": 12.4, "us_aqi": 88 }
            }
        """.trimIndent()

        assertEquals(88, OpenMeteoAirQualityParser.parse(json))
    }

    @Test(expected = Exception::class)
    fun `malformed json throws rather than silently returning bad data`() {
        OpenMeteoAirQualityParser.parse("not json")
    }
}
