package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NwsPointsParserTest {

    @Test
    fun `parses radarStation from a well-formed response`() {
        val json = """{ "properties": { "radarStation": "KJAX" } }"""

        assertEquals("KJAX", NwsPointsParser.parse(json))
    }

    @Test
    fun `radarStation is null when the response omits it`() {
        val json = """{ "properties": {} }"""

        assertNull(NwsPointsParser.parse(json))
    }

    @Test
    fun `ignores unknown fields rather than failing to parse`() {
        val json = """
            {
              "id": "https://api.weather.gov/points/30.1588,-81.6206",
              "properties": {
                "gridId": "JAX",
                "gridX": 55,
                "gridY": 84,
                "radarStation": "KJAX"
              }
            }
        """.trimIndent()

        assertEquals("KJAX", NwsPointsParser.parse(json))
    }
}
