package com.rusty.aurora.layout

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** SharedPreferences-backed, same pattern as SoundLibrary's persisted
 *  custom-sound entries - a single JSON blob under one key, since the whole
 *  layout is always read/written together. */
class LayoutRepositoryImpl(context: Context) : LayoutRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun getLayout(): List<TileConfig> {
        val raw = prefs.getString(KEY_LAYOUT, null) ?: return DEFAULT_TILE_LAYOUT
        val parsed = runCatching { json.decodeFromString<List<TileConfig>>(raw) }.getOrNull()
        return parsed?.takeIf { it.isNotEmpty() } ?: DEFAULT_TILE_LAYOUT
    }

    override fun setLayout(tiles: List<TileConfig>) {
        if (tiles.none { it.visible }) return
        prefs.edit().putString(KEY_LAYOUT, json.encodeToString(tiles)).apply()
    }

    private companion object {
        const val PREFS_NAME = "aurora_layout"
        const val KEY_LAYOUT = "tiles"
    }
}
