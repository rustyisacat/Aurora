package com.rusty.aurora.model

import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.weather.WeatherSnapshot
import kotlinx.serialization.Serializable

/**
 * Snapshot of everything the Echo Show dashboard needs to render in one
 * request. New data sources get added as new fields here - clients simply
 * see new keys appear in the JSON. battery/charging/notifications keep
 * their original meaning and position for backward compatibility; nextAlarm
 * and weather are null (not omitted - see Json.encodeDefaults in
 * DashboardRoute) when no data is available yet, and calendar is always a
 * list, empty when there's nothing to show or permission was denied.
 */
@Serializable
data class DashboardResponse(
    val battery: Int,
    val charging: Boolean,
    val notifications: Int,
    val nextAlarm: NextAlarm? = null,
    val calendar: List<CalendarEvent> = emptyList(),
    val weather: WeatherSnapshot? = null
)
