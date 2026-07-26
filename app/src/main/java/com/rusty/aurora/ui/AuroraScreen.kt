package com.rusty.aurora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rusty.aurora.calendar.CalendarEvent
import com.rusty.aurora.model.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraScreen(
    uiState: AuroraUiState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onCopyDashboardUrl: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestCalendarAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Aurora") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServerStatusCard(uiState)
            BatteryCard(uiState)
            NotificationsCard(uiState)
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartServer,
                    enabled = uiState.serverStatus != ServerStatus.RUNNING
                ) {
                    Text("Start Server")
                }
                OutlinedButton(
                    onClick = onStopServer,
                    enabled = uiState.serverStatus == ServerStatus.RUNNING
                ) {
                    Text("Stop Server")
                }
            }

            OutlinedButton(
                onClick = { uiState.dashboardUrl?.let(onCopyDashboardUrl) },
                enabled = uiState.dashboardUrl != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy Dashboard URL")
            }
        }
    }
}

@Composable
private fun ServerStatusCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Aurora is running", style = MaterialTheme.typography.titleMedium)
            LabeledRow("Server status", uiState.serverStatus.name)
            LabeledRow("Server IP", uiState.localIpAddress ?: "Unavailable")
            LabeledRow("Port", uiState.port.toString())
        }
    }
}

@Composable
private fun BatteryCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Battery", style = MaterialTheme.typography.titleMedium)
            LabeledRow(
                "Level",
                "${uiState.batteryPercent}%" + if (uiState.isCharging) " (charging)" else ""
            )
        }
    }
}

@Composable
private fun NotificationsCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            LabeledRow("Active", uiState.notificationCount.toString())
        }
    }
}

@Composable
private fun WeatherCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Weather", style = MaterialTheme.typography.titleMedium)
            val weather = uiState.weather
            if (weather == null) {
                Text("No data yet", style = MaterialTheme.typography.bodyMedium)
            } else {
                LabeledRow("Temperature", "${weather.temperature}°")
                LabeledRow("Condition", weather.condition)
                LabeledRow("High / Low", "${weather.high}° / ${weather.low}°")
            }
        }
    }
}

@Composable
private fun TodayEventsCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Today's Events", style = MaterialTheme.typography.titleMedium)
            if (uiState.calendarEvents.isEmpty()) {
                Text("No events", style = MaterialTheme.typography.bodyMedium)
            } else {
                uiState.calendarEvents.forEach { event -> EventRow(event) }
            }
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
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NextAlarmCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Next Alarm", style = MaterialTheme.typography.titleMedium)
            Text(
                uiState.nextAlarm?.time ?: "None set",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PermissionCard(message: String, buttonLabel: String, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onGrant) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
