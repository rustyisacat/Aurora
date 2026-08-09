package com.rusty.aurora.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal NWS (api.weather.gov) alerts client: one unauthenticated GET
 * request, same HttpURLConnection-only approach as [OpenMeteoClient]. The
 * NWS API requires a real, identifying User-Agent on every request (it
 * rejects generic/default ones) - unlike Open-Meteo, which needs none.
 */
internal class NwsAlertClient(
    private val latitude: Double,
    private val longitude: Double
) {
    fun fetchActiveAlert(): WeatherAlert? {
        val url = URL("https://api.weather.gov/alerts/active?point=$latitude,$longitude")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/geo+json")

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("NWS alerts returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return NwsAlertParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
        const val USER_AGENT = "Aurora-Dashboard/1.0 (https://github.com/rustyisacat/Aurora)"
    }
}
