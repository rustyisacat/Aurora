package com.rusty.aurora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.rusty.aurora.model.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraScreen(
    uiState: AuroraUiState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onCopyDashboardUrl: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(uiState)

            if (!uiState.hasNotificationAccess) {
                NotificationAccessCard(onRequestNotificationAccess)
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
private fun StatusCard(uiState: AuroraUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Aurora is running", style = MaterialTheme.typography.titleMedium)

            LabeledRow("Server status", uiState.serverStatus.name)
            LabeledRow("Server IP", uiState.localIpAddress ?: "Unavailable")
            LabeledRow("Port", uiState.port.toString())
            LabeledRow(
                "Battery",
                "${uiState.batteryPercent}%" + if (uiState.isCharging) " (charging)" else ""
            )
            LabeledRow("Notifications", uiState.notificationCount.toString())
        }
    }
}

@Composable
private fun NotificationAccessCard(onRequestNotificationAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Notification access is off - the notification count will read 0 until it's granted.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRequestNotificationAccess) {
                Text("Grant Notification Access")
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
