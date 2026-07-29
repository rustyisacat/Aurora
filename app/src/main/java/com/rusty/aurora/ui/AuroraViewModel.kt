package com.rusty.aurora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.layout.LayoutRepository
import com.rusty.aurora.layout.TileConfig
import com.rusty.aurora.layout.TileSize
import com.rusty.aurora.location.LocationRepository
import com.rusty.aurora.model.ServerStatus
import com.rusty.aurora.network.HomeNetworkMonitor
import com.rusty.aurora.network.HomeNetworkRepository
import com.rusty.aurora.notifications.NotificationBlocklistRepository
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.photo.PhotoInfo
import com.rusty.aurora.photo.PhotoRepository
import com.rusty.aurora.photo.WallpaperConfigRepository
import com.rusty.aurora.photo.WallpaperMode
import com.rusty.aurora.photo.WallpaperScheduleEntry
import com.rusty.aurora.profile.UserProfileRepository
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.util.NetworkUtil
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One app the phone app's Notification Apps card can show a toggle for -
 *  [blocked] true means it's excluded from what the dashboard sees. */
data class KnownAppUiState(val packageName: String, val label: String, val blocked: Boolean)

/** Everything the screen needs to render, in one immutable snapshot. */
data class AuroraUiState(
    val serverStatus: ServerStatus = ServerStatus.STOPPED,
    val port: Int = AuroraServerController.DEFAULT_PORT,
    val localIpAddress: String? = null,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val notificationCount: Int = 0,
    val hasNotificationAccess: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = false,
    val hasDndAccess: Boolean = false,
    val isOnHomeNetwork: Boolean = false,
    val homeSubnetPrefix: String? = null,
    val detectedWifiSubnetPrefix: String? = null,
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val calendarShowsTomorrow: Boolean = false,
    val nextAlarm: NextAlarm? = null,
    val weather: WeatherSnapshot? = null,
    val tileLayout: List<TileConfig> = emptyList(),
    val userName: String? = null,
    val knownNotificationApps: List<KnownAppUiState> = emptyList(),
    val wallpaperPhotos: List<PhotoInfo> = emptyList(),
    val wallpaperMode: WallpaperMode = WallpaperMode.ROTATING,
    val wallpaperSinglePhotoId: String? = null,
    val wallpaperSchedule: List<WallpaperScheduleEntry> = emptyList()
) {
    val dashboardUrl: String?
        get() = localIpAddress?.let { ip -> "http://$ip:$port/dashboard" }
}

class AuroraViewModel(
    private val serverController: AuroraServerController,
    private val batteryRepository: BatteryRepository,
    private val notificationCountRepository: NotificationCountRepository,
    private val notificationBlocklistRepository: NotificationBlocklistRepository,
    private val calendarRepository: CalendarRepository,
    private val alarmRepository: AlarmRepository,
    private val weatherRepository: WeatherRepository,
    private val layoutRepository: LayoutRepository,
    private val locationRepository: LocationRepository,
    private val userProfileRepository: UserProfileRepository,
    private val homeNetworkMonitor: HomeNetworkMonitor,
    private val homeNetworkRepository: HomeNetworkRepository,
    private val photoRepository: PhotoRepository,
    private val wallpaperConfigRepository: WallpaperConfigRepository,
    private val hasNotificationAccess: () -> Boolean,
    private val hasPostNotificationsPermission: () -> Boolean,
    private val hasDndAccess: () -> Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuroraUiState(
            tileLayout = layoutRepository.getLayout(),
            userName = userProfileRepository.getUserName(),
            homeSubnetPrefix = homeNetworkRepository.getHomeSubnetPrefix(),
            wallpaperPhotos = photoRepository.getPhotos(),
            wallpaperMode = wallpaperConfigRepository.getMode(),
            wallpaperSinglePhotoId = wallpaperConfigRepository.getSinglePhotoId(),
            wallpaperSchedule = wallpaperConfigRepository.getSchedule()
        )
    )
    val uiState: StateFlow<AuroraUiState> = _uiState.asStateFlow()

    init {
        observeServerStatus()
        observeNotificationCount()
        observeNotificationApps()
        observeHomeNetwork()
        pollDeviceState()
        // Starting/stopping the server itself is AuroraBackgroundService's
        // job now, driven by homeNetworkMonitor - this ViewModel only
        // observes serverController.status for display, it doesn't start
        // or stop it directly.
    }

    /**
     * Called from Activity#onResume and the calendar/location permission
     * callbacks - notification access (system Settings), calendar access,
     * location access, and the notification-post permission (all three
     * runtime dialogs) are all granted outside this ViewModel's control.
     */
    fun refreshPermissionState() {
        _uiState.update {
            it.copy(
                hasNotificationAccess = hasNotificationAccess(),
                hasCalendarPermission = calendarRepository.hasCalendarPermission(),
                hasLocationPermission = locationRepository.hasLocationPermission(),
                hasPostNotificationsPermission = hasPostNotificationsPermission(),
                hasDndAccess = hasDndAccess()
            )
        }
    }

    /**
     * Every mutation below follows the same shape: apply the change to the
     * in-memory list, persist it, then update the UI from the persisted
     * result - so the UI always reflects what LayoutRepository actually
     * accepted (e.g. a rejected "hide the last visible tile" leaves the UI
     * showing the unchanged layout, not a phantom hidden state).
     */
    fun moveTileUp(id: String) = reorderTile(id, offset = -1)

    fun moveTileDown(id: String) = reorderTile(id, offset = 1)

    private fun reorderTile(id: String, offset: Int) {
        val current = _uiState.value.tileLayout
        val index = current.indexOfFirst { it.id == id }
        val targetIndex = index + offset
        if (index < 0 || targetIndex < 0 || targetIndex >= current.size) return

        val reordered = current.toMutableList()
        val tile = reordered.removeAt(index)
        reordered.add(targetIndex, tile)
        persistLayout(reordered)
    }

    fun setTileVisible(id: String, visible: Boolean) {
        persistLayout(_uiState.value.tileLayout.map { if (it.id == id) it.copy(visible = visible) else it })
    }

    fun setTileSize(id: String, size: TileSize) {
        persistLayout(_uiState.value.tileLayout.map { if (it.id == id) it.copy(size = size) else it })
    }

    private fun persistLayout(tiles: List<TileConfig>) {
        layoutRepository.setLayout(tiles)
        _uiState.update { it.copy(tileLayout = layoutRepository.getLayout()) }
    }

    /** Called from the first-launch name prompt, and from "Change Name" later. */
    fun setUserName(name: String) {
        userProfileRepository.setUserName(name)
        _uiState.update { it.copy(userName = userProfileRepository.getUserName()) }
    }

    /** Called from the first-launch home-network prompt, and from "Change
     *  Home Network" later. Forces an immediate recheck - if the phone is
     *  already sitting on the network just configured, no new Wi-Fi
     *  connectivity event will ever fire to trigger one on its own. */
    fun setHomeSubnetPrefix(prefix: String) {
        homeNetworkRepository.setHomeSubnetPrefix(prefix)
        _uiState.update { it.copy(homeSubnetPrefix = homeNetworkRepository.getHomeSubnetPrefix()) }
        homeNetworkMonitor.recheck()
    }

    private fun observeServerStatus() {
        viewModelScope.launch {
            serverController.status.collect { status ->
                _uiState.update { it.copy(serverStatus = status, port = serverController.port) }
            }
        }
    }

    private fun observeNotificationCount() {
        viewModelScope.launch {
            notificationCountRepository.notificationCount.collect { count ->
                _uiState.update { it.copy(notificationCount = count) }
            }
        }
    }

    private fun observeNotificationApps() {
        viewModelScope.launch {
            combine(
                notificationBlocklistRepository.knownApps,
                notificationBlocklistRepository.blockedPackages
            ) { known, blocked ->
                known
                    .map { app -> KnownAppUiState(app.packageName, app.label, blocked = app.packageName in blocked) }
                    .sortedBy { it.label.lowercase() }
            }.collect { apps ->
                _uiState.update { it.copy(knownNotificationApps = apps) }
            }
        }
    }

    fun setWallpaperMode(mode: WallpaperMode) {
        wallpaperConfigRepository.setMode(mode)
        _uiState.update { it.copy(wallpaperMode = mode) }
    }

    fun setWallpaperSinglePhoto(photoId: String) {
        wallpaperConfigRepository.setSinglePhotoId(photoId)
        _uiState.update { it.copy(wallpaperSinglePhotoId = photoId) }
    }

    /** Replaces any existing entry at the same [time] rather than adding a
     *  second one - two photos scheduled for the same moment is never a
     *  sensible state, so "add" and "edit" are the same operation here. */
    fun setWallpaperScheduleEntry(time: String, photoId: String) {
        val updated = wallpaperConfigRepository.getSchedule().filterNot { it.time == time } +
            WallpaperScheduleEntry(photoId, time)
        val sorted = updated.sortedBy { it.time }
        wallpaperConfigRepository.setSchedule(sorted)
        _uiState.update { it.copy(wallpaperSchedule = sorted) }
    }

    fun removeWallpaperScheduleEntry(time: String) {
        val updated = wallpaperConfigRepository.getSchedule().filterNot { it.time == time }
        wallpaperConfigRepository.setSchedule(updated)
        _uiState.update { it.copy(wallpaperSchedule = updated) }
    }

    /** Toggling this doesn't just persist the preference - it also asks the
     *  listener service to recompute right away (see
     *  NotificationCountRepository.refresh), so the change shows up on the
     *  dashboard's very next poll instead of waiting for that app's next
     *  notification event. */
    fun toggleAppBlocked(packageName: String, blocked: Boolean) {
        notificationBlocklistRepository.setBlocked(packageName, blocked)
        notificationCountRepository.refresh()
    }

    /** AuroraBackgroundService is the one actually starting/stopping the
     *  server off this same signal - this is purely for the status card. */
    private fun observeHomeNetwork() {
        viewModelScope.launch {
            homeNetworkMonitor.isOnHomeNetwork.collect { onHomeNetwork ->
                _uiState.update { it.copy(isOnHomeNetwork = onHomeNetwork) }
            }
        }
    }

    /**
     * None of battery, calendar, alarm, or weather have a cheap "observe changes"
     * API to replace this with, so all four are refreshed together on a simple
     * poll. Runs on Dispatchers.IO rather than the viewModelScope default
     * (Main.immediate) because CalendarRepository.getEvents() is a real,
     * blocking ContentResolver query - the v0.1 version of this loop only ever
     * touched cheap BatteryManager/Settings reads, so it didn't need this.
     */
    private fun pollDeviceState() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _uiState.update {
                    it.copy(
                        batteryPercent = batteryRepository.getBatteryLevelPercent(),
                        isCharging = batteryRepository.isCharging(),
                        hasNotificationAccess = hasNotificationAccess(),
                        hasCalendarPermission = calendarRepository.hasCalendarPermission(),
                        hasLocationPermission = locationRepository.hasLocationPermission(),
                        hasPostNotificationsPermission = hasPostNotificationsPermission(),
                        hasDndAccess = hasDndAccess(),
                        localIpAddress = NetworkUtil.getLocalIpAddress(),
                        detectedWifiSubnetPrefix = homeNetworkMonitor.currentWifiSubnetPrefix(),
                        calendarEvents = calendarRepository.getEvents(),
                        calendarShowsTomorrow = calendarRepository.isShowingTomorrow(),
                        nextAlarm = alarmRepository.getNextAlarm(),
                        weather = weatherRepository.getWeather(),
                        // Read fresh each poll rather than only right after
                        // setWallpaperMode/etc - MainActivity writes new
                        // photo selections straight to photoRepository
                        // without going through this ViewModel (same as the
                        // sound/ambient photo pickers), so this is what
                        // picks that up.
                        wallpaperPhotos = photoRepository.getPhotos()
                    )
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    class Factory(
        private val serverController: AuroraServerController,
        private val batteryRepository: BatteryRepository,
        private val notificationCountRepository: NotificationCountRepository,
        private val notificationBlocklistRepository: NotificationBlocklistRepository,
        private val calendarRepository: CalendarRepository,
        private val alarmRepository: AlarmRepository,
        private val weatherRepository: WeatherRepository,
        private val layoutRepository: LayoutRepository,
        private val locationRepository: LocationRepository,
        private val userProfileRepository: UserProfileRepository,
        private val homeNetworkMonitor: HomeNetworkMonitor,
        private val homeNetworkRepository: HomeNetworkRepository,
        private val photoRepository: PhotoRepository,
        private val wallpaperConfigRepository: WallpaperConfigRepository,
        private val hasNotificationAccess: () -> Boolean,
        private val hasPostNotificationsPermission: () -> Boolean,
        private val hasDndAccess: () -> Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuroraViewModel(
                serverController,
                batteryRepository,
                notificationCountRepository,
                notificationBlocklistRepository,
                calendarRepository,
                alarmRepository,
                weatherRepository,
                layoutRepository,
                locationRepository,
                userProfileRepository,
                homeNetworkMonitor,
                homeNetworkRepository,
                photoRepository,
                wallpaperConfigRepository,
                hasNotificationAccess,
                hasPostNotificationsPermission,
                hasDndAccess
            ) as T
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5000L
    }
}
