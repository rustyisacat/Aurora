package com.rusty.aurora.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Open-Meteo air quality client: one unauthenticated GET request
 * against their separate air-quality-api host, same HttpURLConnection-only
 * approach as [OpenMeteoClient].
 */
internal class OpenMeteoAirQualityClient(
    private val latitude: Double,
    private val longitude: Double
) {
    fun fetchUsAqi(): Int? {
        val url = URL(
            "https://air-quality-api.open-meteo.com/v1/air-quality" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=us_aqi" +
                "&timezone=auto"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Open-Meteo air quality returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return OpenMeteoAirQualityParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
