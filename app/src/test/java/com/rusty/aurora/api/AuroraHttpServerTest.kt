package com.rusty.aurora.api

import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private class FakeCalendarRepository(private val events: List<CalendarEvent>) : CalendarRepository {
        override fun hasCalendarPermission(): Boolean = true
        override fun getTodayEvents(): List<CalendarEvent> = events
    }

    private class FakeAlarmRepository(private val alarm: NextAlarm?) : AlarmRepository {
        override fun getNextAlarm(): NextAlarm? = alarm
    }

    private class FakeWeatherRepository(private val weather: WeatherSnapshot?) : WeatherRepository {
        override fun getWeather(): WeatherSnapshot? = weather
    }

    private var server: AuroraHttpServer? = null

    @After
    fun stopServer() {
        server?.stop()
    }

    private fun startServer(
        calendarEvents: List<CalendarEvent> = emptyList(),
        nextAlarm: NextAlarm? = null,
        weather: WeatherSnapshot? = null
    ): String {
        val routes = listOf(
            HealthRoute(),
            DashboardRoute(
                batteryRepository = FakeBatteryRepository(level = 77, charging = true),
                notificationCountRepository = NotificationCountRepositoryImpl().apply { setCount(3) },
                calendarRepository = FakeCalendarRepository(calendarEvents),
                alarmRepository = FakeAlarmRepository(nextAlarm),
                weatherRepository = FakeWeatherRepository(weather)
            )
        )

        // Port 0: the OS assigns a free ephemeral port, so tests never collide
        // with each other or with a real Aurora instance running locally.
        val newServer = AuroraHttpServer(port = 0, routes = routes)
        newServer.start(AuroraHttpServer.DEFAULT_SOCKET_TIMEOUT_MS, true)
        server = newServer
        return "http://localhost:${newServer.listeningPort}"
    }

    @Test
    fun `health endpoint returns 200 and plain text OK body`() {
        val baseUrl = startServer()
        val connection = get("$baseUrl/health")

        assertEquals(200, connection.responseCode)
        assertEquals("Aurora OK", connection.inputStream.bufferedReader().readText())
    }

    @Test
    fun `dashboard endpoint includes battery, notifications, alarm, calendar, and weather`() {
        val baseUrl = startServer(
            calendarEvents = listOf(CalendarEvent(title = "School", start = "08:00", end = "15:00", allDay = false)),
            nextAlarm = NextAlarm(time = "07:00", enabled = true),
            weather = WeatherSnapshot(temperature = 74, condition = "Clear", high = 86, low = 68)
        )
        val connection = get("$baseUrl/dashboard")
        val body = connection.inputStream.bufferedReader().readText()

        assertEquals(200, connection.responseCode)
        assertTrue(connection.contentType.startsWith("application/json"))
        assertEquals(
            """{"battery":77,"charging":true,"notifications":3,""" +
                """"nextAlarm":{"time":"07:00","enabled":true},""" +
                """"calendar":[{"title":"School","start":"08:00","end":"15:00","allDay":false}],""" +
                """"weather":{"temperature":74,"condition":"Clear","high":86,"low":68}}""",
            body
        )
    }

    @Test
    fun `dashboard endpoint represents absent alarm and weather as explicit json null, not missing keys`() {
        val baseUrl = startServer(calendarEvents = emptyList(), nextAlarm = null, weather = null)
        val connection = get("$baseUrl/dashboard")
        val body = connection.inputStream.bufferedReader().readText()

        assertEquals(
            """{"battery":77,"charging":true,"notifications":3,""" +
                """"nextAlarm":null,"calendar":[],"weather":null}""",
            body
        )
    }

    @Test
    fun `unknown route returns 404`() {
        val baseUrl = startServer()
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
