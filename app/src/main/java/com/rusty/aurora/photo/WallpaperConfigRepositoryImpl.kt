package com.rusty.aurora.photo

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** SharedPreferences-backed, same pattern as PhotoRepositoryImpl/
 *  NotificationBlocklistRepositoryImpl - small persisted state, no reason
 *  for anything heavier than a couple of prefs keys. */
class WallpaperConfigRepositoryImpl(private val context: Context) : WallpaperConfigRepository {

    override fun getMode(): WallpaperMode =
        prefs.getString(KEY_MODE, null)?.let { name ->
            runCatching { WallpaperMode.valueOf(name) }.getOrNull()
        } ?: WallpaperMode.ROTATING

    override fun setMode(mode: WallpaperMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    override fun getSinglePhotoId(): String? = prefs.getString(KEY_SINGLE_PHOTO_ID, null)

    override fun setSinglePhotoId(photoId: String?) {
        prefs.edit().putString(KEY_SINGLE_PHOTO_ID, photoId).apply()
    }

    override fun getSchedule(): List<WallpaperScheduleEntry> {
        val raw = prefs.getString(KEY_SCHEDULE, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<WallpaperScheduleEntry>>(raw) }.getOrDefault(emptyList())
    }

    override fun setSchedule(entries: List<WallpaperScheduleEntry>) {
        prefs.edit().putString(KEY_SCHEDULE, json.encodeToString(entries)).apply()
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_NAME = "aurora_wallpaper_config"
        const val KEY_MODE = "mode"
        const val KEY_SINGLE_PHOTO_ID = "single_photo_id"
        const val KEY_SCHEDULE = "schedule"
        val json = Json { ignoreUnknownKeys = true }
    }
}
