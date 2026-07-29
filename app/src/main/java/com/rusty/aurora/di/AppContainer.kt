package com.rusty.aurora.di

import android.content.Context
import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.AlarmRepositoryImpl
import com.rusty.aurora.api.AuroraHttpServer
import com.rusty.aurora.api.ClearNotificationsRoute
import com.rusty.aurora.api.DashboardRoute
import com.rusty.aurora.api.DeleteWakeAlarmRoute
import com.rusty.aurora.api.DismissWakeAlarmRoute
import com.rusty.aurora.api.GetWakeAlarmsRoute
import com.rusty.aurora.api.HealthRoute
import com.rusty.aurora.api.PauseSoundRoute
import com.rusty.aurora.api.PhotoLibraryRoute
import com.rusty.aurora.api.PhotoStreamRoute
import com.rusty.aurora.api.PlaySoundRoute
import com.rusty.aurora.api.SetSleepTimerRoute
import com.rusty.aurora.api.SetVolumeRoute
import com.rusty.aurora.api.SetDefaultAlarmSoundRoute
import com.rusty.aurora.api.SetWakeAlarmRoute
import com.rusty.aurora.api.SnoozeWakeAlarmRoute
import com.rusty.aurora.api.SoundLibraryRoute
import com.rusty.aurora.api.SoundStreamRoute
import com.rusty.aurora.api.StopSoundRoute
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.battery.BatteryRepositoryImpl
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.calendar.CalendarRepositoryImpl
import com.rusty.aurora.layout.LayoutRepository
import com.rusty.aurora.layout.LayoutRepositoryImpl
import com.rusty.aurora.location.LocationRepository
import com.rusty.aurora.location.LocationRepositoryImpl
import com.rusty.aurora.network.HomeNetworkMonitor
import com.rusty.aurora.network.HomeNetworkMonitorImpl
import com.rusty.aurora.network.HomeNetworkRepository
import com.rusty.aurora.network.HomeNetworkRepositoryImpl
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.photo.PhotoRepository
import com.rusty.aurora.photo.PhotoRepositoryImpl
import com.rusty.aurora.profile.UserProfileRepository
import com.rusty.aurora.profile.UserProfileRepositoryImpl
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.sound.SoundLibrary
import com.rusty.aurora.sound.SoundRepository
import com.rusty.aurora.sound.SoundRepositoryImpl
import com.rusty.aurora.wakealarm.WakeAlarmRepository
import com.rusty.aurora.wakealarm.WakeAlarmRepositoryImpl
import com.rusty.aurora.wakealarm.WakeAlarmScheduler
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherRepositoryImpl

/**
 * Hand-rolled composition root.
 *
 * Ten repositories plus a network monitor is still comfortably within
 * "wire it by hand" territory -
 * a DI framework would add build time and APK size for no benefit yet.
 * Every class still takes its dependencies through its constructor, so the
 * option to introduce one later (or swap in fakes for tests) stays open.
 */
class AppContainer(context: Context) {

    val batteryRepository: BatteryRepository = BatteryRepositoryImpl(context)

    val notificationCountRepository: NotificationCountRepository = NotificationCountRepositoryImpl()

    val calendarRepository: CalendarRepository = CalendarRepositoryImpl(context)

    // Hoisted to a shared instance (rather than built inline for
    // SoundRepositoryImpl, as before) so WakeAlarmRepositoryImpl can also
    // read it - it needs a sensible library-wide fallback sound for alarms
    // that have neither their own soundId nor a configured default.
    val soundLibrary: SoundLibrary = SoundLibrary(context)

    val wakeAlarmRepository: WakeAlarmRepository =
        WakeAlarmRepositoryImpl(context, WakeAlarmScheduler(context), soundLibrary)

    val alarmRepository: AlarmRepository = AlarmRepositoryImpl(context, wakeAlarmRepository)

    val locationRepository: LocationRepository = LocationRepositoryImpl(context)

    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(context, locationRepository)

    // Aurora never plays audio itself - it tracks desired sound machine
    // state and serves the raw bytes (SoundStreamRoute); the Echo Show's
    // kiosk browser is the actual audio engine. See SoundRepository's doc
    // comment.
    val soundRepository: SoundRepository =
        SoundRepositoryImpl(context, soundLibrary, alarmRepository)

    val layoutRepository: LayoutRepository = LayoutRepositoryImpl(context)

    val userProfileRepository: UserProfileRepository = UserProfileRepositoryImpl(context)

    // Ambient Mode's photo background - see PhotoRepository's doc comment.
    // Aurora never renders anything itself; it just persists which photos
    // were picked and serves their bytes (PhotoStreamRoute).
    val photoRepository: PhotoRepository = PhotoRepositoryImpl(context)

    val homeNetworkRepository: HomeNetworkRepository = HomeNetworkRepositoryImpl(context)

    val homeNetworkMonitor: HomeNetworkMonitor = HomeNetworkMonitorImpl(context, homeNetworkRepository)

    private val routes = listOf(
        HealthRoute(),
        DashboardRoute(
            batteryRepository = batteryRepository,
            notificationCountRepository = notificationCountRepository,
            calendarRepository = calendarRepository,
            alarmRepository = alarmRepository,
            weatherRepository = weatherRepository,
            soundRepository = soundRepository,
            wakeAlarmRepository = wakeAlarmRepository,
            layoutRepository = layoutRepository,
            userProfileRepository = userProfileRepository
        ),
        PlaySoundRoute(soundRepository),
        PauseSoundRoute(soundRepository),
        StopSoundRoute(soundRepository),
        SetVolumeRoute(soundRepository),
        SetSleepTimerRoute(soundRepository),
        SoundLibraryRoute(soundRepository),
        SoundStreamRoute(soundRepository),
        GetWakeAlarmsRoute(wakeAlarmRepository),
        SetWakeAlarmRoute(wakeAlarmRepository),
        DeleteWakeAlarmRoute(wakeAlarmRepository),
        DismissWakeAlarmRoute(wakeAlarmRepository),
        SnoozeWakeAlarmRoute(wakeAlarmRepository),
        SetDefaultAlarmSoundRoute(wakeAlarmRepository),
        ClearNotificationsRoute(notificationCountRepository),
        PhotoLibraryRoute(photoRepository),
        PhotoStreamRoute(photoRepository)
    )

    val serverController = AuroraServerController { port -> AuroraHttpServer(port, routes) }
}
