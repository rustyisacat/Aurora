package com.rusty.aurora.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Relative size on the dashboard's card grid - see echo-dashboard's
 *  script.js TILE_SIZE_WEIGHT for the exact flex-grow ratio each maps to. */
@Serializable
enum class TileSize {
    @SerialName("small") SMALL,
    @SerialName("medium") MEDIUM,
    @SerialName("large") LARGE
}

/**
 * One dashboard tile's customization state: whether it's shown at all, and
 * its relative size. [id] must match one of the fixed tile ids the
 * dashboard knows about (see DEFAULT_TILE_LAYOUT) - an id it doesn't
 * recognize is just skipped, so this stays forward-compatible if tiles are
 * ever added or removed in a future version.
 */
@Serializable
data class TileConfig(
    val id: String,
    val visible: Boolean = true,
    val size: TileSize = TileSize.MEDIUM
)

/** The Morning Overview page's six card-grid tiles, in their original
 *  hand-tuned order and sizes - the fallback shown until the phone app is
 *  used to customize anything. */
val DEFAULT_TILE_LAYOUT = listOf(
    TileConfig("weather", visible = true, size = TileSize.MEDIUM),
    TileConfig("phone", visible = true, size = TileSize.MEDIUM),
    TileConfig("notifications", visible = true, size = TileSize.LARGE),
    TileConfig("schedule", visible = true, size = TileSize.MEDIUM),
    TileConfig("alarm", visible = true, size = TileSize.SMALL),
    TileConfig("sound", visible = true, size = TileSize.LARGE)
)

/** Human-readable labels for the phone app's customization screen - the
 *  dashboard itself already has its own titles baked into index.html. */
val TILE_DISPLAY_NAMES = mapOf(
    "weather" to "Weather",
    "phone" to "Phone",
    "notifications" to "Notifications",
    "schedule" to "Today's Schedule",
    "alarm" to "Next Alarm",
    "sound" to "Sound Machine"
)
