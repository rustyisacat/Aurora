package com.rusty.aurora.api

import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exercises AuroraHttpServer end-to-end over real HTTP. None of api/ or the
 * repository interfaces touch the Android framework, so this runs as a
 * plain JVM unit test - no emulator or device required to prove the server
 * actually starts and answers requests correctly.
 */
class AuroraHttpServerTest {

    private class FakeBatteryRepository(
        private val level: Int,
        private val charging: Boolean
    ) : BatteryRepository {
        override fun getBatteryLevelPercent(): Int = level
        override fun isCharging(): Boolean = charging
    }

    private lateinit var server: AuroraHttpServer
    private lateinit var baseUrl: String

    @Before
    fun startServer() {
        val batteryRepository = FakeBatteryRepository(level = 77, charging = true)
        val notificationCountRepository = NotificationCountRepositoryImpl().apply { setCount(3) }
        val routes = listOf(
            HealthRoute(),
            DashboardRoute(batteryRepository, notificationCountRepository)
        )

        // Port 0: the OS assigns a free ephemeral port, so tests never collide
        // with each other or with a real Aurora instance running locally.
        server = AuroraHttpServer(port = 0, routes = routes)
        server.start(AuroraHttpServer.DEFAULT_SOCKET_TIMEOUT_MS, true)
        baseUrl = "http://localhost:${server.listeningPort}"
    }

    @After
    fun stopServer() {
        server.stop()
    }

    @Test
    fun `health endpoint returns 200 and plain text OK body`() {
        val connection = get("$baseUrl/health")

        assertEquals(200, connection.responseCode)
        assertEquals("Aurora OK", connection.inputStream.bufferedReader().readText())
    }

    @Test
    fun `dashboard endpoint returns battery, charging, and notification count as JSON`() {
        val connection = get("$baseUrl/dashboard")
        val body = connection.inputStream.bufferedReader().readText()

        assertEquals(200, connection.responseCode)
        assertTrue(connection.contentType.startsWith("application/json"))
        assertEquals(
            """{"battery":77,"charging":true,"notifications":3}""",
            body
        )
    }

    @Test
    fun `unknown route returns 404`() {
        val connection = get("$baseUrl/nonexistent")

        assertEquals(404, connection.responseCode)
    }

    private fun get(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2000
            readTimeout = 2000
        }
}
