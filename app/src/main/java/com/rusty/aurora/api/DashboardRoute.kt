package com.rusty.aurora.api

import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.layout.LayoutRepository
import com.rusty.aurora.model.DashboardResponse
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.profile.UserProfileRepository
import com.rusty.aurora.sound.SoundRepository
import com.rusty.aurora.wakealarm.WakeAlarmRepository
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
    private val weatherRepository: WeatherRepository,
    private val soundRepository: SoundRepository,
    private val wakeAlarmRepository: WakeAlarmRepository,
    private val layoutRepository: LayoutRepository,
    private val userProfileRepository: UserProfileRepository
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
            notificationGroups = notificationCountRepository.notificationGroups.value,
            nextAlarm = alarmRepository.getNextAlarm(),
            calendar = calendarRepository.getEvents(),
            calendarShowsTomorrow = calendarRepository.isShowingTomorrow(),
            weather = weatherRepository.getWeather(),
            soundMachine = soundRepository.getState(),
            wakeAlarms = wakeAlarmRepository.getAlarms(),
            wakeAlarmRinging = wakeAlarmRepository.getRingingState(),
            defaultAlarmSoundId = wakeAlarmRepository.getDefaultAlarmSoundId(),
            layout = layoutRepository.getLayout(),
            userName = userProfileRepository.getUserName()
        )
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            json.encodeToString(body)
        )
    }
}
