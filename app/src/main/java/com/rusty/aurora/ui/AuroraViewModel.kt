package com.rusty.aurora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rusty.aurora.alarm.AlarmRepository
import com.rusty.aurora.alarm.NextAlarm
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.calendar.CalendarRepository
import com.rusty.aurora.model.ServerStatus
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.util.NetworkUtil
import com.rusty.aurora.weather.WeatherRepository
import com.rusty.aurora.weather.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val nextAlarm: NextAlarm? = null,
    val weather: WeatherSnapshot? = null
) {
    val dashboardUrl: String?
        get() = localIpAddress?.let { ip -> "http://$ip:$port/dashboard" }
}

class AuroraViewModel(
    private val serverController: AuroraServerController,
    private val batteryRepository: BatteryRepository,
    private val notificationCountRepository: NotificationCountRepository,
    private val calendarRepository: CalendarRepository,
    private val alarmRepository: AlarmRepository,
    private val weatherRepository: WeatherRepository,
    private val hasNotificationAccess: () -> Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuroraUiState())
    val uiState: StateFlow<AuroraUiState> = _uiState.asStateFlow()

    init {
        observeServerStatus()
        observeNotificationCount()
        pollDeviceState()
        startServer()
    }

    fun startServer() {
        serverController.start()
        _uiState.update { it.copy(localIpAddress = NetworkUtil.getLocalIpAddress()) }
    }

    fun stopServer() {
        serverController.stop()
    }

    /**
     * Called from Activity#onResume and the calendar permission callback - both
     * notification access (system Settings) and calendar access (runtime dialog)
     * are granted outside this ViewModel's control.
     */
    fun refreshPermissionState() {
        _uiState.update {
            it.copy(
                hasNotificationAccess = hasNotificationAccess(),
                hasCalendarPermission = calendarRepository.hasCalendarPermission()
            )
        }
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

    /**
     * None of battery, calendar, alarm, or weather have a cheap "observe changes"
     * API to replace this with, so all four are refreshed together on a simple
     * poll. Runs on Dispatchers.IO rather than the viewModelScope default
     * (Main.immediate) because CalendarRepository.getTodayEvents() is a real,
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
                        calendarEvents = calendarRepository.getTodayEvents(),
                        nextAlarm = alarmRepository.getNextAlarm(),
                        weather = weatherRepository.getWeather()
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
        private val calendarRepository: CalendarRepository,
        private val alarmRepository: AlarmRepository,
        private val weatherRepository: WeatherRepository,
        private val hasNotificationAccess: () -> Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuroraViewModel(
                serverController,
                batteryRepository,
                notificationCountRepository,
                calendarRepository,
                alarmRepository,
                weatherRepository,
                hasNotificationAccess
            ) as T
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5000L
    }
}
