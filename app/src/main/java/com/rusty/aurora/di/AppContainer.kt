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
import com.rusty.aurora.api.GetHomeNetworkRoute
import com.rusty.aurora.api.HealthRoute
import com.rusty.aurora.api.KnownNotificationAppsRoute
import com.rusty.aurora.api.NotificationIconRoute
import com.rusty.aurora.api.PauseSoundRoute
import com.rusty.aurora.api.PhotoLibraryRoute
import com.rusty.aurora.api.PhotoStreamRoute
import com.rusty.aurora.api.PlaySoundRoute
import com.rusty.aurora.api.SetSleepTimerRoute
import com.rusty.aurora.api.SetVolumeRoute
import com.rusty.aurora.api.SetDefaultAlarmSoundRoute
import com.rusty.aurora.api.SetHomeNetworkRoute
import com.rusty.aurora.api.SetLayoutRoute
import com.rusty.aurora.api.SetNotificationBlockedRoute
import com.rusty.aurora.api.SetUserNameRoute
import com.rusty.aurora.api.SetWakeAlarmRoute
import com.rusty.aurora.api.SetWallpaperModeRoute
import com.rusty.aurora.api.SetWallpaperScheduleRoute
import com.rusty.aurora.api.SetWallpaperSinglePhotoRoute
import com.rusty.aurora.api.SnoozeWakeAlarmRoute
import com.rusty.aurora.api.SetDndRoute
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
import com.rusty.aurora.network.AuroraNsdAdvertiser
import com.rusty.aurora.network.HomeNetworkMonitor
import com.rusty.aurora.network.HomeNetworkMonitorImpl
import com.rusty.aurora.network.HomeNetworkRepository
import com.rusty.aurora.network.HomeNetworkRepositoryImpl
import com.rusty.aurora.notifications.AppIconProvider
import com.rusty.aurora.notifications.AppIconProviderImpl
import com.rusty.aurora.notifications.DndRepository
import com.rusty.aurora.notifications.DndRepositoryImpl
import com.rusty.aurora.notifications.NotificationBlocklistRepository
import com.rusty.aurora.notifications.NotificationBlocklistRepositoryImpl
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.notifications.NotificationCountRepositoryImpl
import com.rusty.aurora.photo.PhotoRepository
import com.rusty.aurora.photo.PhotoRepositoryImpl
import com.rusty.aurora.photo.WallpaperConfigRepository
import com.rusty.aurora.photo.WallpaperConfigRepositoryImpl
import com.rusty.aurora.profile.UserProfileRepository
import com.rusty.aurora.profile.UserProfileRepositoryImpl
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.sound.SoundLibrary
import com.rusty.aurora.sound.SoundRepository
import com.rusty.aurora.sound.SoundRepositoryImpl
import com.rusty.aurora.wakealarm.WakeAlarmRepository
import com.rusty.aurora.wakealarm.WakeAlarmRepositoryImpl
import com.rusty.aurora.wakealarm.WakeAlarmScheduler
import com.rusty.aurora.weather.WeatherAlertRepository
import com.rusty.aurora.weather.WeatherAlertRepositoryImpl
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

    val notificationBlocklistRepository: NotificationBlocklistRepository = NotificationBlocklistRepositoryImpl(context)

    val appIconProvider: AppIconProvider = AppIconProviderImpl(context)

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

    val weatherAlertRepository: WeatherAlertRepository =
        WeatherAlertRepositoryImpl(context, locationRepository)

    // Aurora never plays audio itself - it tracks desired sound machine
    // state and serves the raw bytes (SoundStreamRoute); the Echo Show's
    // kiosk browser is the actual audio engine. See SoundRepository's doc
    // comment.
    val soundRepository: SoundRepository =
        SoundRepositoryImpl(context, soundLibrary, alarmRepository)

    val layoutRepository: LayoutRepository = LayoutRepositoryImpl(context)

    val userProfileRepository: UserProfileRepository = UserProfileRepositoryImpl(context)

    // The Echo Show's dashboard wallpaper and Ambient Mode's photo
    // background both cycle through this same picked-photo set now - see
    // PhotoRepository's doc comment. Aurora never renders anything itself;
    // it just persists which photos were picked and serves their bytes
    // (PhotoStreamRoute).
    val photoRepository: PhotoRepository = PhotoRepositoryImpl(context)

    val wallpaperConfigRepository: WallpaperConfigRepository = WallpaperConfigRepositoryImpl(context)

    val dndRepository: DndRepository = DndRepositoryImpl(context)

    val homeNetworkRepository: HomeNetworkRepository = HomeNetworkRepositoryImpl(context)

    val homeNetworkMonitor: HomeNetworkMonitor = HomeNetworkMonitorImpl(context, homeNetworkRepository)

    val nsdAdvertiser = AuroraNsdAdvertiser(context)

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
            userProfileRepository = userProfileRepository,
            dndRepository = dndRepository,
            wallpaperConfigRepository = wallpaperConfigRepository,
            weatherAlertRepository = weatherAlertRepository
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
        PhotoStreamRoute(photoRepository),
        SetDndRoute(dndRepository),
        NotificationIconRoute(appIconProvider),
        SetUserNameRoute(userProfileRepository),
        GetHomeNetworkRoute(homeNetworkRepository),
        SetHomeNetworkRoute(homeNetworkRepository),
        KnownNotificationAppsRoute(notificationBlocklistRepository),
        SetNotificationBlockedRoute(notificationBlocklistRepository, notificationCountRepository),
        SetWallpaperModeRoute(wallpaperConfigRepository),
        SetWallpaperSinglePhotoRoute(wallpaperConfigRepository),
        SetWallpaperScheduleRoute(wallpaperConfigRepository),
        SetLayoutRoute(layoutRepository)
    )

    val serverController = AuroraServerController { port -> AuroraHttpServer(port, routes) }
}
