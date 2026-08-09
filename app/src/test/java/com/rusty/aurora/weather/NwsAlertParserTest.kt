package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NwsAlertParserTest {

    @Test
    fun `no features means no alert`() {
        val json = """{ "features": [] }"""

        assertNull(NwsAlertParser.parse(json))
    }

    @Test
    fun `a single feature is parsed into a WeatherAlert`() {
        val json = """
            {
              "features": [
                {
                  "properties": {
                    "event": "Flood Watch",
                    "headline": "Flood Watch issued for the area",
                    "severity": "Moderate"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            WeatherAlert(event = "Flood Watch", headline = "Flood Watch issued for the area", severity = "Moderate"),
            NwsAlertParser.parse(json)
        )
    }

    @Test
    fun `multiple features picks the highest-severity one regardless of list order`() {
        val json = """
            {
              "features": [
                { "properties": { "event": "Flood Watch", "headline": "h1", "severity": "Minor" } },
                { "properties": { "event": "Tornado Warning", "headline": "h2", "severity": "Extreme" } },
                { "properties": { "event": "Heat Advisory", "headline": "h3", "severity": "Moderate" } }
              ]
            }
        """.trimIndent()

        val result = NwsAlertParser.parse(json)

        assertEquals("Tornado Warning", result?.event)
        assertEquals("Extreme", result?.severity)
    }

    @Test
    fun `a known severity outranks Unknown`() {
        val json = """
            {
              "features": [
                { "properties": { "event": "Special Weather Statement", "headline": "h1", "severity": "Unknown" } },
                { "properties": { "event": "Winter Storm Warning", "headline": "h2", "severity": "Severe" } }
              ]
            }
        """.trimIndent()

        assertEquals("Winter Storm Warning", NwsAlertParser.parse(json)?.event)
    }

    @Test
    fun `falls back to event when headline is missing or blank`() {
        val json = """
            {
              "features": [
                { "properties": { "event": "Air Quality Alert", "severity": "Minor" } }
              ]
            }
        """.trimIndent()

        assertEquals("Air Quality Alert", NwsAlertParser.parse(json)?.headline)
    }
}
