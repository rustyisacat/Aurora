package com.rusty.aurora.model

import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.layout.DEFAULT_TILE_LAYOUT
import com.rusty.aurora.layout.TileConfig
import com.rusty.aurora.notifications.NotificationGroup
import com.rusty.aurora.sound.SoundMachineState
import com.rusty.aurora.weather.WeatherSnapshot
import kotlinx.serialization.Serializable

/**
 * Snapshot of everything the Echo Show dashboard needs to render in one
 * request. New data sources get added as new fields here - clients simply
 * see new keys appear in the JSON. battery/charging/notifications keep
 * their original meaning and position for backward compatibility -
 * notificationGroups is additive, not a replacement; nextAlarm and weather
 * are null (not omitted - see Json.encodeDefaults in DashboardRoute) when
 * no data is available yet, and calendar/notificationGroups are always a
 * list, empty when there's nothing to show. soundMachine is never null -
 * "not playing, nothing loaded" is a well-defined state, not an absence of
 * data. layout is the Morning Overview page's card order/visibility/size,
 * customized from the phone app (LayoutRepository) - the dashboard only
 * ever reads this, it never writes it back. userName is null until the
 * first-launch name prompt has been answered (UserProfileRepository) - the
 * Morning Briefing greets without a name until then.
 */
@Serializable
data class DashboardResponse(
    val battery: Int,
    val charging: Boolean,
    val notifications: Int,
    val notificationGroups: List<NotificationGroup> = emptyList(),
    val nextAlarm: NextAlarm? = null,
    val calendar: List<CalendarEvent> = emptyList(),
    val weather: WeatherSnapshot? = null,
    val soundMachine: SoundMachineState,
    val layout: List<TileConfig> = DEFAULT_TILE_LAYOUT,
    val userName: String? = null
)
