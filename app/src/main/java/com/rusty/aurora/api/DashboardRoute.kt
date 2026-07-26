package com.rusty.aurora.api

import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.model.DashboardResponse
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.weather.WeatherRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DashboardRoute(
    private val batteryRepository: BatteryRepository,
    private val notificationCountRepository: NotificationCountRepository,
    private val calendarRepository: CalendarRepository,
    private val alarmRepository: AlarmRepository,
    private val weatherRepository: WeatherRepository
) : Route {

    // encodeDefaults=true keeps nextAlarm/weather present as explicit JSON
    // nulls (rather than omitted keys) when there's no data - a stable
    // shape is easier for the Echo Show's JS to consume than optional keys.
    private val json = Json { encodeDefaults = true }

    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/dashboard"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = DashboardResponse(
            battery = batteryRepository.getBatteryLevelPercent(),
            charging = batteryRepository.isCharging(),
            notifications = notificationCountRepository.notificationCount.value,
            nextAlarm = alarmRepository.getNextAlarm(),
            calendar = calendarRepository.getTodayEvents(),
            weather = weatherRepository.getWeather()
        )
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            json.encodeToString(body)
        )
    }
}
