package com.rusty.aurora.di

import android.content.Context
import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.AlarmRepositoryImpl
import com.rusty.aurora.api.AuroraHttpServer
import com.rusty.aurora.api.DashboardRoute
import com.rusty.aurora.api.HealthRoute
import com.rusty.aurora.api.PauseSoundRoute
import com.rusty.aurora.api.PlaySoundRoute
import com.rusty.aurora.api.SetSleepTimerRoute
import com.rusty.aurora.api.SetVolumeRoute
import com.rusty.aurora.api.SoundLibraryRoute
import com.rusty.aurora.api.SoundStreamRoute
import com.rusty.aurora.api.StopSoundRoute
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.battery.BatteryRepositoryImpl
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.calendar.CalendarRepositoryImpl
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.sound.SoundLibrary
import com.rusty.aurora.sound.SoundRepository
import com.rusty.aurora.sound.SoundRepositoryImpl
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherRepositoryImpl

/**
 * Hand-rolled composition root.
 *
 * Seven repositories is still comfortably within "wire it by hand" territory -
 * a DI framework would add build time and APK size for no benefit yet.
 * Every class still takes its dependencies through its constructor, so the
 * option to introduce one later (or swap in fakes for tests) stays open.
 */
class AppContainer(context: Context) {

    val batteryRepository: BatteryRepository = BatteryRepositoryImpl(context)

    val notificationCountRepository: NotificationCountRepository = NotificationCountRepositoryImpl()

    val calendarRepository: CalendarRepository = CalendarRepositoryImpl(context)

    val alarmRepository: AlarmRepository = AlarmRepositoryImpl(context)

    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(context)

    // Aurora never plays audio itself - it tracks desired sound machine
    // state and serves the raw bytes (SoundStreamRoute); the Echo Show's
    // kiosk browser is the actual audio engine. See SoundRepository's doc
    // comment.
    val soundRepository: SoundRepository =
        SoundRepositoryImpl(context, SoundLibrary(context), alarmRepository)

    private val routes = listOf(
        HealthRoute(),
        DashboardRoute(
            batteryRepository = batteryRepository,
            notificationCountRepository = notificationCountRepository,
            calendarRepository = calendarRepository,
            alarmRepository = alarmRepository,
            weatherRepository = weatherRepository,
            soundRepository = soundRepository
        ),
        PlaySoundRoute(soundRepository),
        PauseSoundRoute(soundRepository),
        StopSoundRoute(soundRepository),
        SetVolumeRoute(soundRepository),
        SetSleepTimerRoute(soundRepository),
        SoundLibraryRoute(soundRepository),
        SoundStreamRoute(soundRepository)
    )

    val serverController = AuroraServerController { port -> AuroraHttpServer(port, routes) }
}
