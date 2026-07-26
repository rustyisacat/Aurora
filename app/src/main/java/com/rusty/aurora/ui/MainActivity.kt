package com.rusty.aurora.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import com.rusty.aurora.AuroraApplication
import com.rusty.aurora.ui.theme.AuroraTheme
import com.rusty.aurora.util.NotificationAccessUtil

/**
 * Thin shell: wires the ViewModel to Compose and forwards the two
 * platform calls (clipboard, Settings intent) it can't own itself.
 * Everything else - server lifecycle, battery/notification state - lives
 * in [AuroraViewModel] and the repositories behind it.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AuroraViewModel by viewModels {
        val container = (application as AuroraApplication).container
        AuroraViewModel.Factory(
            serverController = container.serverController,
            batteryRepository = container.batteryRepository,
            notificationCountRepository = container.notificationCountRepository,
            hasNotificationAccess = { NotificationAccessUtil.isNotificationAccessGranted(this) }
        )
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
                    onRequestNotificationAccess = ::openNotificationAccessSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notification access is granted outside the app, so re-check on return.
        viewModel.refreshNotificationAccessState()
    }

    private fun copyDashboardUrlToClipboard(url: String) {
        getSystemService<ClipboardManager>()
            ?.setPrimaryClip(ClipData.newPlainText("Aurora dashboard URL", url))
    }

    private fun openNotificationAccessSettings() {
        startActivity(NotificationAccessUtil.buildNotificationAccessSettingsIntent())
    }
}
