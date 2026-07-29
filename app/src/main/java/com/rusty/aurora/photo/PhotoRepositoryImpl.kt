package com.rusty.aurora.photo

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** SharedPreferences-backed, same pattern as SoundLibrary's persisted
 *  custom-sound entries - a single JSON blob under one key, since the
 *  whole selection is always read/written together. */
class PhotoRepositoryImpl(private val context: Context) : PhotoRepository {

    override fun getPhotos(): List<PhotoInfo> = loadEntries().map { it.toPhotoInfo() }

    override fun setPhotos(uris: List<Uri>) {
        val entries = uris.map { uri ->
            PhotoEntry(id = "photo_${uri.toString().hashCode()}", uriString = uri.toString())
        }
        prefs.edit().putString(KEY_PHOTOS, json.encodeToString(entries)).apply()
    }

    override fun openPhotoStream(id: String): PhotoStream? {
        val entry = loadEntries().firstOrNull { it.id == id } ?: return null
        return runCatching {
            val uri = Uri.parse(entry.uriString)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            PhotoStream(bytes, mimeType)
        }.getOrNull()
    }

    private fun loadEntries(): List<PhotoEntry> {
        val raw = prefs.getString(KEY_PHOTOS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PhotoEntry>>(raw) }.getOrDefault(emptyList())
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Serializable
    private data class PhotoEntry(val id: String, val uriString: String) {
        fun toPhotoInfo() = PhotoInfo(id, uriString)
    }

    private companion object {
        const val PREFS_NAME = "aurora_photos"
        const val KEY_PHOTOS = "photos"
        val json = Json { ignoreUnknownKeys = true }
    }
}
