package com.rusty.aurora.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import com.rusty.aurora.AuroraApplication
import com.rusty.aurora.ui.theme.AuroraTheme
import com.rusty.aurora.util.NotificationAccessUtil

/**
 * Thin shell: wires the ViewModel to Compose and forwards the platform
 * calls it can't own itself (clipboard, Settings intent, the calendar
 * permission dialog). Everything else - server lifecycle, battery/
 * notification/calendar/alarm/weather state - lives in [AuroraViewModel]
 * and the repositories behind it.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AuroraViewModel by viewModels {
        val container = (application as AuroraApplication).container
        AuroraViewModel.Factory(
            serverController = container.serverController,
            batteryRepository = container.batteryRepository,
            notificationCountRepository = container.notificationCountRepository,
            calendarRepository = container.calendarRepository,
            alarmRepository = container.alarmRepository,
            weatherRepository = container.weatherRepository,
            hasNotificationAccess = { NotificationAccessUtil.isNotificationAccessGranted(this) }
        )
    }

    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Granted or denied, just refresh state - a denial should never crash
            // or need special-casing here, CalendarRepository already degrades to
            // an empty event list on its own.
            viewModel.refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuroraTheme {
                val uiState by viewModel.uiState.collectAsState()
                AuroraScreen(
                    uiState = uiState,
                    onStartServer = viewModel::startServer,
                    onStopServer = viewModel::stopServer,
                    onCopyDashboardUrl = ::copyDashboardUrlToClipboard,
                    onRequestNotificationAccess = ::openNotificationAccessSettings,
                    onRequestCalendarAccess = ::requestCalendarPermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notification access and calendar access are both granted outside this
        // screen (system Settings / the permission dialog), so re-check on return.
        viewModel.refreshPermissionState()
    }

    private fun copyDashboardUrlToClipboard(url: String) {
        getSystemService<ClipboardManager>()
            ?.setPrimaryClip(ClipData.newPlainText("Aurora dashboard URL", url))
    }

    private fun openNotificationAccessSettings() {
        startActivity(NotificationAccessUtil.buildNotificationAccessSettingsIntent())
    }

    private fun requestCalendarPermission() {
        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
    }
}
