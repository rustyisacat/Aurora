package com.rusty.aurora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rusty.aurora.ui.theme.AuroraTextSecondary

/**
 * Shown before the main screen on first launch (AuroraUiState.homeSubnetPrefix
 * is null until this is answered), and again later if the user taps "Change
 * Home Network" - same composable either way, MainActivity decides when.
 *
 * Aurora only runs its dashboard server while on this network (see
 * HomeNetworkMonitor), and this is what makes that check work on anyone's
 * router, not just one hardcoded at build time - important since this app
 * is public source, not a private config file.
 */
@Composable
fun HomeNetworkEntryScreen(
    detectedSubnetPrefix: String?,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(detectedSubnetPrefix) { mutableStateOf(detectedSubnetPrefix ?: "") }
    val normalized = normalizeSubnetPrefix(text)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Set Your Home Network",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (detectedSubnetPrefix != null) {
                    "We detected you're on the $detectedSubnetPrefix* network. Aurora only " +
                        "runs its dashboard server while connected to this Wi-Fi - confirm " +
                        "or edit it below."
                } else {
                    "Connect to your home Wi-Fi to auto-detect it, or enter its subnet " +
                        "manually below. Aurora only runs its dashboard server while " +
                        "connected to this network."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = AuroraTextSecondary
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Home network, e.g. 192.168.1.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { normalized?.let(onSubmit) },
                enabled = normalized != null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

/** "192.168.1" or "192.168.1." -> "192.168.1."; null if not three valid
 *  0-255 octets. */
private fun normalizeSubnetPrefix(input: String): String? {
    val octets = input.trim().removeSuffix(".").split(".")
    if (octets.size != 3) return null
    if (octets.any { octet -> octet.toIntOrNull()?.let { it in 0..255 } != true }) return null
    return octets.joinToString(".", postfix = ".")
}
