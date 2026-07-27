package com.rusty.aurora.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import com.rusty.aurora.AuroraApplication
import com.rusty.aurora.ui.theme.AuroraTheme
import com.rusty.aurora.util.NotificationAccessUtil

/**
 * Thin shell: wires the ViewModel to Compose and forwards the platform
 * calls it can't own itself (clipboard, Settings intent, the calendar and
 * location permission dialogs, the sound machine's custom-file picker).
 * Everything else - server lifecycle, battery/notification/calendar/alarm/
 * weather state - lives in [AuroraViewModel] and the repositories behind it.
 *
 * Custom sound import is handled directly here rather than through
 * AuroraViewModel, same as clipboard/Settings-intent above - it's a
 * one-shot platform action (Storage Access Framework), not ongoing UI
 * state to track.
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
            layoutRepository = container.layoutRepository,
            locationRepository = container.locationRepository,
            userProfileRepository = container.userProfileRepository,
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

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Same as calendar above - a denial just leaves weather on its fixed
            // fallback coordinate, never a crash.
            viewModel.refreshPermissionState()
        }

    private val importSoundLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importCustomSound(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            // Three screens, no real back stack to speak of - plain booleans
            // are simpler and more reliable here than pulling in
            // Navigation-Compose for what's really just "show a different
            // screen, then come back."
            var showCustomizeScreen by remember { mutableStateOf(false) }
            var forceNameEntry by remember { mutableStateOf(false) }

            AuroraTheme(weatherCondition = uiState.weather?.condition) {
                when {
                    // Blocks everything else until answered - first launch has
                    // no name yet, and uiState.userName stays null until this
                    // is submitted at least once.
                    uiState.userName.isNullOrBlank() || forceNameEntry -> {
                        NameEntryScreen(
                            initialValue = uiState.userName ?: "",
                            onSubmit = { name ->
                                viewModel.setUserName(name)
                                forceNameEntry = false
                            }
                        )
                    }
                    showCustomizeScreen -> {
                        CustomizeLayoutScreen(
                            tiles = uiState.tileLayout,
                            onBack = { showCustomizeScreen = false },
                            onMoveUp = viewModel::moveTileUp,
                            onMoveDown = viewModel::moveTileDown,
                            onVisibleChange = viewModel::setTileVisible,
                            onSizeChange = viewModel::setTileSize
                        )
                    }
                    else -> {
                        AuroraScreen(
                            uiState = uiState,
                            onStartServer = viewModel::startServer,
                            onStopServer = viewModel::stopServer,
                            onCopyDashboardUrl = ::copyDashboardUrlToClipboard,
                            onRequestNotificationAccess = ::openNotificationAccessSettings,
                            onRequestCalendarAccess = ::requestCalendarPermission,
                            onRequestLocationAccess = ::requestLocationPermission,
                            onImportCustomSound = ::launchCustomSoundPicker,
                            onCustomizeDashboard = { showCustomizeScreen = true },
                            onChangeName = { forceNameEntry = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notification, calendar, and location access are all granted outside
        // this screen (system Settings / the permission dialogs), so re-check
        // on return.
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

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun launchCustomSoundPicker() {
        importSoundLauncher.launch(
            arrayOf("audio/mpeg", "audio/ogg", "audio/flac", "audio/x-flac", "audio/wav", "audio/x-wav")
        )
    }

    private fun importCustomSound(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Custom sound"
        (application as AuroraApplication).container.soundRepository.importCustomSound(uri, displayName)
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
