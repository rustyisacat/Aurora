package com.rusty.aurora.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Minimal NWS `/points/{lat},{lon}` client - resolves which radar station
 * covers a coordinate, same HttpURLConnection-only approach and required
 * User-Agent as [NwsAlertClient]. The NWS API rejects points with more
 * than 4 decimal digits of precision, so the coordinate is truncated
 * before building the URL.
 */
internal class NwsPointsClient(
    private val latitude: Double,
    private val longitude: Double
) {
    fun fetchRadarStation(): String? {
        val url = URL(
            "https://api.weather.gov/points/" +
                "${"%.4f".format(Locale.US, latitude)},${"%.4f".format(Locale.US, longitude)}"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/geo+json")

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("NWS points returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return NwsPointsParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000
        const val USER_AGENT = "Aurora-Dashboard/1.0 (https://github.com/rustyisacat/Aurora)"
    }
}
