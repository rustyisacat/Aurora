package com.rusty.aurora.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Open-Meteo client: one unauthenticated GET request. No HTTP
 * library added for this - same reasoning as choosing NanoHTTPD over a
 * full server framework on the inbound side (see api/AuroraHttpServer).
 */
internal class OpenMeteoClient(
    private val latitude: Double,
    private val longitude: Double
) {
    fun fetchCurrentWeather(): WeatherSnapshot {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,weather_code" +
                "&hourly=precipitation_probability" +
                "&temperature_unit=fahrenheit" +
                "&timezone=auto" +
                "&forecast_days=${WeatherConfig.FORECAST_DAYS}"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Open-Meteo returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return OpenMeteoResponseParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
    }
}
