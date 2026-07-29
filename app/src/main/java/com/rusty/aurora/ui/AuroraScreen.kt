package com.rusty.aurora.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.model.ServerStatus
import com.rusty.aurora.photo.PhotoInfo
import com.rusty.aurora.photo.WallpaperMode
import com.rusty.aurora.photo.WallpaperScheduleEntry
import com.rusty.aurora.ui.theme.AuroraTextSecondary
import com.rusty.aurora.ui.theme.AuroraTextTertiary

/**
 * The phone-side control screen, styled to match the Echo Show dashboard's
 * own look (see echo-dashboard/style.css) rather than default Material:
 * true-black background, dark elevated cards with an uppercase icon+title
 * header, and a weather-driven accent color (set by AuroraTheme) used the
 * same way the dashboard uses it - highlighted values and icon tint.
 */
@Composable
fun AuroraScreen(
    uiState: AuroraUiState,
    onCopyDashboardUrl: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestCalendarAccess: () -> Unit,
    onRequestLocationAccess: () -> Unit,
    onRequestPostNotificationsPermission: () -> Unit,
    onImportCustomSound: () -> Unit,
    onChoosePhotos: () -> Unit,
    onRequestDndAccess: () -> Unit,
    onToggleAppBlocked: (String, Boolean) -> Unit,
    onSetWallpaperMode: (WallpaperMode) -> Unit,
    onSetWallpaperSinglePhoto: (String) -> Unit,
    onSetWallpaperScheduleEntry: (String, String) -> Unit,
    onRemoveWallpaperScheduleEntry: (String) -> Unit,
    onCustomizeDashboard: () -> Unit,
    onChangeName: () -> Unit,
    onChangeHomeNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(uiState)

            ServerStatusCard(uiState)
            BatteryCard(uiState)
            NotificationsCard(uiState)
            if (uiState.knownNotificationApps.isNotEmpty()) {
                NotificationAppsCard(uiState, onToggleAppBlocked)
            }
            WeatherCard(uiState)
            TodayEventsCard(uiState)
            NextAlarmCard(uiState)

            if (!uiState.hasNotificationAccess) {
                PermissionCard(
                    message = "Notification access is off - the notification count will read 0 until it's granted.",
                    buttonLabel = "Grant Notification Access",
                    onGrant = onRequestNotificationAccess
                )
            }
            if (!uiState.hasCalendarPermission) {
                PermissionCard(
                    message = "Calendar access is off - today's events will read empty until it's granted.",
                    buttonLabel = "Grant Calendar Access",
                    onGrant = onRequestCalendarAccess
                )
            }
            if (!uiState.hasLocationPermission) {
                PermissionCard(
                    message = "Location access is off - weather falls back to a fixed location " +
                        "until it's granted, instead of following the phone automatically.",
                    buttonLabel = "Grant Location Access",
                    onGrant = onRequestLocationAccess
                )
            }
            if (!uiState.hasPostNotificationsPermission) {
                PermissionCard(
                    message = "Notifications are off - the background service that keeps the " +
                        "dashboard fed still runs without it, but you won't see its status icon.",
                    buttonLabel = "Allow Notifications",
                    onGrant = onRequestPostNotificationsPermission
                )
            }
            if (!uiState.hasDndAccess) {
                PermissionCard(
                    message = "Do Not Disturb access is off - the dashboard's DND toggle won't be " +
                        "able to actually change it until it's granted.",
                    buttonLabel = "Grant Do Not Disturb Access",
                    onGrant = onRequestDndAccess
                )
            }

            OutlinedButton(
                onClick = { uiState.dashboardUrl?.let(onCopyDashboardUrl) },
                enabled = uiState.dashboardUrl != null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy Dashboard URL")
            }

            // Custom sound import needs an on-device file picker (Storage Access
            // Framework), which only this screen can launch - the web dashboard
            // has no access to the phone's filesystem. Playback control itself
            // stays on the dashboard; this is a one-time setup action, which is
            // what this screen is for.
            OutlinedButton(
                onClick = onImportCustomSound,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import Custom Sound")
            }

            // Drives both Ambient Mode's photo rotation and the main
            // dashboard's wallpaper (which also extracts its accent color
            // from whatever's picked) - one shared photo set for both, not
            // two separate pickers. Needs the on-device Photo Picker, which
            // only this screen can launch - same reasoning as the custom
            // sound import above. No storage permission needed: the modern
            // Photo Picker grants read access to just the images actually
            // selected. Replaces the whole set each time it's run, rather
            // than adding to it - the picker itself has no way to show
            // which photos are already chosen to add alongside.
            OutlinedButton(
                onClick = onChoosePhotos,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose Photos")
            }

            if (uiState.wallpaperPhotos.isNotEmpty()) {
                WallpaperCard(
                    uiState = uiState,
                    onSetMode = onSetWallpaperMode,
                    onSetSinglePhoto = onSetWallpaperSinglePhoto,
                    onSetScheduleEntry = onSetWallpaperScheduleEntry,
                    onRemoveScheduleEntry = onRemoveWallpaperScheduleEntry
                )
            }

            Button(
                onClick = onCustomizeDashboard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Customize Dashboard")
            }

            OutlinedButton(
                onClick = onChangeName,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Change Name")
            }

            OutlinedButton(
                onClick = onChangeHomeNetwork,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Change Home Network")
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ScreenHeader(uiState: AuroraUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Aurora",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        StatusDot(uiState)
    }
}

/** Same idea as the dashboard's .status-line dot: a quiet colored dot
 *  rather than a loud badge. AuroraBackgroundService owns the actual
 *  start/stop decision (see its doc comment) - this just reflects it. */
@Composable
private fun StatusDot(uiState: AuroraUiState) {
    val running = uiState.serverStatus == ServerStatus.RUNNING
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (running) MaterialTheme.colorScheme.primary else AuroraTextTertiary,
                    shape = CircleShape
                )
        )
        Text(
            serverStatusLabel(uiState),
            style = MaterialTheme.typography.labelMedium,
            color = AuroraTextSecondary
        )
    }
}

private fun serverStatusLabel(uiState: AuroraUiState): String = when {
    uiState.serverStatus == ServerStatus.RUNNING -> "Running"
    uiState.isOnHomeNetwork -> "Starting…"
    else -> "Idle - away from home Wi-Fi"
}

/** One shared card shape - icon-slot + uppercase title, then whatever
 *  content the caller supplies - mirrors .card/.card-title in the
 *  dashboard's own stylesheet, so every card here has a dashboard card's
 *  counterpart. */
@Composable
private fun AuroraCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AuroraTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AuroraTextSecondary
                )
            }
            content()
        }
    }
}

@Composable
private fun ServerStatusCard(uiState: AuroraUiState) {
    AuroraCard(title = "Aurora Server", icon = Icons.Outlined.Dns) {
        LabeledRow("Status", serverStatusLabel(uiState))
        LabeledRow("Home Wi-Fi", if (uiState.isOnHomeNetwork) "Connected" else "Not connected")
        LabeledRow("Address", uiState.localIpAddress?.let { "$it:${uiState.port}" } ?: "Unavailable")
    }
}

@Composable
private fun BatteryCard(uiState: AuroraUiState) {
    AuroraCard(
        title = "Battery",
        icon = if (uiState.isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd
    ) {
        HeroValue("${uiState.batteryPercent}%")
        Text(
            if (uiState.isCharging) "Charging" else "Not charging",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
    }
}

@Composable
private fun NotificationsCard(uiState: AuroraUiState) {
    AuroraCard(title = "Notifications", icon = Icons.Outlined.Notifications) {
        HeroValue(uiState.notificationCount.toString())
    }
}

/** Every app that's ever posted a notification, each with a switch for
 *  whether the dashboard gets to see it - stays listed even once blocked
 *  (or once its notifications are all cleared), see
 *  NotificationBlocklistRepository.knownApps. */
@Composable
private fun NotificationAppsCard(uiState: AuroraUiState, onToggleBlocked: (String, Boolean) -> Unit) {
    AuroraCard(title = "Notification Apps", icon = Icons.Outlined.Notifications) {
        uiState.knownNotificationApps.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = !app.blocked,
                    onCheckedChange = { shown -> onToggleBlocked(app.packageName, !shown) }
                )
            }
        }
    }
}

/** Controls what the dashboard's main-screen wallpaper shows - rotating
 *  through the whole library (the original, still-default behavior), one
 *  fixed photo, or a schedule of photo changes by time of day. Only shown
 *  once at least one photo's been picked (see "Choose Photos" above) -
 *  there's nothing to configure with an empty library. */
@Composable
private fun WallpaperCard(
    uiState: AuroraUiState,
    onSetMode: (WallpaperMode) -> Unit,
    onSetSinglePhoto: (String) -> Unit,
    onSetScheduleEntry: (String, String) -> Unit,
    onRemoveScheduleEntry: (String) -> Unit
) {
    AuroraCard(title = "Wallpaper", icon = Icons.Outlined.Wallpaper) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rotate automatically", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.wallpaperMode == WallpaperMode.ROTATING,
                onCheckedChange = { rotating ->
                    // Turning rotation off has to land somewhere concrete -
                    // Single is the simpler of the two remaining modes, and
                    // the chips below let it be changed to Schedule right
                    // away if that's what's actually wanted.
                    onSetMode(if (rotating) WallpaperMode.ROTATING else WallpaperMode.SINGLE)
                }
            )
        }

        if (uiState.wallpaperMode != WallpaperMode.ROTATING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.wallpaperMode == WallpaperMode.SINGLE,
                    onClick = { onSetMode(WallpaperMode.SINGLE) },
                    label = { Text("Single") }
                )
                FilterChip(
                    selected = uiState.wallpaperMode == WallpaperMode.SCHEDULED,
                    onClick = { onSetMode(WallpaperMode.SCHEDULED) },
                    label = { Text("Schedule") }
                )
            }

            when (uiState.wallpaperMode) {
                WallpaperMode.SINGLE -> {
                    if (uiState.wallpaperSinglePhotoId == null) {
                        Text(
                            "Tap a photo to use it as the wallpaper.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextSecondary
                        )
                    }
                    PhotoThumbnailRow(
                        photos = uiState.wallpaperPhotos,
                        selectedId = uiState.wallpaperSinglePhotoId,
                        onSelect = onSetSinglePhoto
                    )
                }
                WallpaperMode.SCHEDULED -> WallpaperScheduleEditor(
                    photos = uiState.wallpaperPhotos,
                    schedule = uiState.wallpaperSchedule,
                    onAdd = onSetScheduleEntry,
                    onRemove = onRemoveScheduleEntry
                )
                WallpaperMode.ROTATING -> Unit
            }
        }
    }
}

@Composable
private fun PhotoThumbnailRow(photos: List<PhotoInfo>, selectedId: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos, key = { it.id }) { photo ->
            val selected = photo.id == selectedId
            AsyncImage(
                model = Uri.parse(photo.uriString),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(photo.id) }
            )
        }
    }
}

@Composable
private fun WallpaperScheduleEditor(
    photos: List<PhotoInfo>,
    schedule: List<WallpaperScheduleEntry>,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (schedule.isEmpty()) {
            Text(
                "No scheduled changes yet - add one below. The dashboard shows " +
                    "whichever entry's time has most recently passed.",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary
            )
        } else {
            schedule.forEach { entry ->
                val photo = photos.firstOrNull { it.id == entry.photoId }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (photo != null) {
                        AsyncImage(
                            model = Uri.parse(photo.uriString),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Text(
                        formatTime12h(entry.time),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemove(entry.time) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showAddDialog = true },
            enabled = photos.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Scheduled Wallpaper")
        }
    }

    if (showAddDialog) {
        AddScheduleEntryDialog(
            photos = photos,
            onConfirm = { time, photoId ->
                onAdd(time, photoId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleEntryDialog(
    photos: List<PhotoInfo>,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(is24Hour = false)
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Add Scheduled Wallpaper",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                TimePicker(state = timePickerState)
                Text("Photo", style = MaterialTheme.typography.labelMedium, color = AuroraTextSecondary)
                PhotoThumbnailRow(
                    photos = photos,
                    selectedId = selectedPhotoId,
                    onSelect = { selectedPhotoId = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val photoId = selectedPhotoId ?: return@TextButton
                            val time = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                            onConfirm(time, photoId)
                        },
                        enabled = selectedPhotoId != null
                    ) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(uiState: AuroraUiState) {
    AuroraCard(title = "Weather", icon = weatherIconFor(uiState.weather?.condition)) {
        val weather = uiState.weather
        if (weather == null) {
            Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
        } else {
            HeroValue("${weather.temperature}°")
            Text(weather.condition, style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledInline("H", "${weather.high}°")
                LabeledInline("L", "${weather.low}°")
            }
            if (weather.sunrise != null && weather.sunset != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledInline("Sunrise", formatTime12h(weather.sunrise))
                    LabeledInline("Sunset", formatTime12h(weather.sunset))
                }
            }
        }
    }
}

/** Matches script.js's WEATHER_ICON_BY_CONDITION - keep the two in sync. */
private fun weatherIconFor(condition: String?): ImageVector = when (condition) {
    "Clear" -> Icons.Outlined.WbSunny
    "Drizzle", "Rain", "Rain Showers" -> Icons.Outlined.Grain
    "Snow", "Snow Showers" -> Icons.Outlined.AcUnit
    "Thunderstorm" -> Icons.Outlined.Bolt
    else -> Icons.Outlined.Cloud // Partly Cloudy, Overcast, Fog, and the no-data default
}

@Composable
private fun TodayEventsCard(uiState: AuroraUiState) {
    val title = if (uiState.calendarShowsTomorrow) "Tomorrow's Events" else "Today's Events"
    AuroraCard(title = title, icon = Icons.Outlined.CalendarToday) {
        if (uiState.calendarEvents.isEmpty()) {
            Text("No events", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
        } else {
            uiState.calendarEvents.forEach { event -> EventRow(event) }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(event.title, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (event.allDay) "All day" else "${event.start}–${event.end}",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
    }
}

@Composable
private fun NextAlarmCard(uiState: AuroraUiState) {
    AuroraCard(title = "Next Alarm", icon = Icons.Outlined.Alarm) {
        HeroValue(uiState.nextAlarm?.time?.let(::formatTime12h) ?: "None set")
    }
}

/** Aurora's repositories hand back 24-hour "HH:mm" strings; matches
 *  script.js's formatTime12h() so the phone and the dashboard always show
 *  a time the same way. */
private fun formatTime12h(hhmm: String): String {
    val parts = hhmm.split(":")
    val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = parts.getOrNull(1) ?: return hhmm
    val period = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return "$hour12:$minute $period"
}

@Composable
private fun PermissionCard(message: String, buttonLabel: String, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(buttonLabel)
            }
        }
    }
}

/** The one big number per card - battery %, temperature, alarm time - same
 *  role as .weather-temp/.phone-battery/.alarm-time in the dashboard CSS. */
@Composable
private fun HeroValue(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "H 96°" / "L 79°" - matches .weather-range's <span>H <b>96°</b></span>. */
@Composable
private fun LabeledInline(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
