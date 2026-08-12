package com.rusty.aurora.model

import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.calendar.WeekDay
import com.rusty.aurora.layout.DEFAULT_TILE_LAYOUT
import com.rusty.aurora.layout.TileConfig
import com.rusty.aurora.notifications.NotificationGroup
import com.rusty.aurora.photo.WallpaperMode
import com.rusty.aurora.photo.WallpaperScheduleEntry
import com.rusty.aurora.sound.SoundMachineState
import com.rusty.aurora.wakealarm.WakeAlarm
import com.rusty.aurora.wakealarm.WakeAlarmRingingState
import com.rusty.aurora.weather.WeatherAlert
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
 * Morning Briefing greets without a name until then. calendar holds
 * today's events before noon and tomorrow's from noon onward (see
 * CalendarRepository), with calendarShowsTomorrow telling the client which
 * one it's looking at so it can label the list correctly. wakeAlarms is
 * Aurora's own alarm list (separate from nextAlarm, which only reflects
 * the phone's stock Clock app for display - see AlarmRepository for how
 * the two are reconciled for scheduling purposes); wakeAlarmRinging tells
 * the dashboard when to actually start blasting one, and is never null for
 * the same "well-defined state, not an absence of data" reason as
 * soundMachine. defaultAlarmSoundId is null until the user picks one from
 * the dashboard - see WakeAlarmRepository.getDefaultAlarmSoundId.
 * weatherAlert is the single highest-severity active NWS alert for the
 * current location, null when there's nothing active (the common case) or
 * nothing fetched yet - see WeatherAlertRepository.
 */
@Serializable
data class DashboardResponse(
    val battery: Int,
    val charging: Boolean,
    val notifications: Int,
    val notificationGroups: List<NotificationGroup> = emptyList(),
    val nextAlarm: NextAlarm? = null,
    val calendar: List<CalendarEvent> = emptyList(),
    val calendarShowsTomorrow: Boolean = false,
    // Today through six days out - independent of calendar/
    // calendarShowsTomorrow's own today-or-tomorrow window, for the
    // dashboard's separate weekly view. See CalendarRepository.getWeekEvents.
    val weekCalendar: List<WeekDay> = emptyList(),
    val weather: WeatherSnapshot? = null,
    val soundMachine: SoundMachineState,
    val wakeAlarms: List<WakeAlarm> = emptyList(),
    val wakeAlarmRinging: WakeAlarmRingingState,
    val defaultAlarmSoundId: String? = null,
    val layout: List<TileConfig> = DEFAULT_TILE_LAYOUT,
    val userName: String? = null,
    // Reflects the phone's actual system-wide interruption filter, not
    // just "did Aurora turn it on" - true if DND is on for any reason,
    // including the user flipping it manually. See DndRepository.
    val dndEnabled: Boolean = false,
    // Null while not charging, or until BatteryRepository has enough
    // charge-rate history for a stable estimate - see ChargingEtaCalculator.
    val chargingEtaMinutes: Int? = null,
    // Which photo (from PhotoRepository's shared library) the dashboard's
    // main-screen wallpaper shows, and how: ROTATING ignores the other two
    // fields entirely (the dashboard cycles the whole library itself, as
    // before); SINGLE uses wallpaperSinglePhotoId; SCHEDULED uses
    // wallpaperSchedule - see WallpaperConfigRepository.
    val wallpaperMode: WallpaperMode = WallpaperMode.ROTATING,
    val wallpaperSinglePhotoId: String? = null,
    val wallpaperSchedule: List<WallpaperScheduleEntry> = emptyList(),
    val weatherAlert: WeatherAlert? = null
)
