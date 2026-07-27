package com.rusty.aurora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rusty.aurora.layout.TILE_DISPLAY_NAMES
import com.rusty.aurora.layout.TileConfig
import com.rusty.aurora.layout.TileSize
import com.rusty.aurora.ui.theme.AuroraTextSecondary

/**
 * Lets Rusty reorder, hide, and resize the Echo Show dashboard's Morning
 * Overview cards from the phone. Every change is persisted immediately
 * (LayoutRepository, via AuroraViewModel) - the dashboard picks it up on
 * its next poll (up to ~30s later), the same way it already picks up
 * Sound Machine state changes.
 *
 * Reordering is up/down buttons rather than drag-and-drop: a fiddly custom
 * drag gesture is more likely to misfire on a kiosk-adjacent settings
 * screen than a plain, always-reliable button tap.
 */
@Composable
fun CustomizeLayoutScreen(
    tiles: List<TileConfig>,
    onBack: () -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onVisibleChange: (String, Boolean) -> Unit,
    onSizeChange: (String, TileSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCount = tiles.count { it.visible }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Customize Dashboard",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                "Reorder, hide, or resize the cards on the Echo Show's Morning " +
                    "Overview page. Changes reach the dashboard within about 30 seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tiles.forEachIndexed { index, tile ->
                    TileEditorRow(
                        tile = tile,
                        canMoveUp = index > 0,
                        canMoveDown = index < tiles.lastIndex,
                        canHide = visibleCount > 1,
                        onMoveUp = { onMoveUp(tile.id) },
                        onMoveDown = { onMoveDown(tile.id) },
                        onVisibleChange = { onVisibleChange(tile.id, it) },
                        onSizeChange = { onSizeChange(tile.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TileEditorRow(
    tile: TileConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canHide: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    onSizeChange: (TileSize) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                    }
                }
                Text(
                    TILE_DISPLAY_NAMES[tile.id] ?: tile.id,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // A tile can only be switched off if it isn't the last visible
                // one - the dashboard always needs at least one card to show,
                // and LayoutRepository would silently reject the change anyway,
                // so this keeps the UI honest about what will actually happen.
                Switch(
                    checked = tile.visible,
                    onCheckedChange = onVisibleChange,
                    enabled = canHide || !tile.visible
                )
            }

            if (tile.visible) {
                SizePicker(selected = tile.size, onSelect = onSizeChange)
            }
        }
    }
}

@Composable
private fun SizePicker(selected: TileSize, onSelect: (TileSize) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TileSize.entries.forEach { size ->
            FilterChip(
                selected = size == selected,
                onClick = { onSelect(size) },
                label = { Text(size.name.lowercase().replaceFirstChar(Char::uppercase)) }
            )
        }
    }
}
