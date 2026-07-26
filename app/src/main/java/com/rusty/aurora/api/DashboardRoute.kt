package com.rusty.aurora.api

import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.model.DashboardResponse
import com.rusty.aurora.notifications.NotificationCountRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DashboardRoute(
    private val batteryRepository: BatteryRepository,
    private val notificationCountRepository: NotificationCountRepository
) : Route {

    private val json = Json { encodeDefaults = true }

    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/dashboard"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = DashboardResponse(
            battery = batteryRepository.getBatteryLevelPercent(),
            charging = batteryRepository.isCharging(),
            notifications = notificationCountRepository.notificationCount.value
        )
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            json.encodeToString(body)
        )
    }
}
