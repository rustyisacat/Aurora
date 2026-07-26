package com.rusty.aurora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rusty.aurora.battery.BatteryRepository
import com.rusty.aurora.model.ServerStatus
import com.rusty.aurora.notifications.NotificationCountRepository
import com.rusty.aurora.service.AuroraServerController
import com.rusty.aurora.util.NetworkUtil
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
    val hasNotificationAccess: Boolean = false
) {
    val dashboardUrl: String?
        get() = localIpAddress?.let { ip -> "http://$ip:$port/dashboard" }
}

class AuroraViewModel(
    private val serverController: AuroraServerController,
    private val batteryRepository: BatteryRepository,
    private val notificationCountRepository: NotificationCountRepository,
    private val hasNotificationAccess: () -> Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuroraUiState())
    val uiState: StateFlow<AuroraUiState> = _uiState.asStateFlow()

    init {
        observeServerStatus()
        observeNotificationCount()
        pollBatteryAndPermissionState()
        startServer()
    }

    fun startServer() {
        serverController.start()
        _uiState.update { it.copy(localIpAddress = NetworkUtil.getLocalIpAddress()) }
    }

    fun stopServer() {
        serverController.stop()
    }

    /** Called from Activity#onResume - access is granted outside the app, in system Settings. */
    fun refreshNotificationAccessState() {
        _uiState.update { it.copy(hasNotificationAccess = hasNotificationAccess()) }
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

    /** Battery has no equivalent "collect changes" API cheap enough to justify a receiver here; poll it. */
    private fun pollBatteryAndPermissionState() {
        viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(
                        batteryPercent = batteryRepository.getBatteryLevelPercent(),
                        isCharging = batteryRepository.isCharging(),
                        hasNotificationAccess = hasNotificationAccess()
                    )
                }
                delay(BATTERY_POLL_INTERVAL_MS)
            }
        }
    }

    class Factory(
        private val serverController: AuroraServerController,
        private val batteryRepository: BatteryRepository,
        private val notificationCountRepository: NotificationCountRepository,
        private val hasNotificationAccess: () -> Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuroraViewModel(
                serverController,
                batteryRepository,
                notificationCountRepository,
                hasNotificationAccess
            ) as T
        }
    }

    private companion object {
        const val BATTERY_POLL_INTERVAL_MS = 5000L
    }
}
