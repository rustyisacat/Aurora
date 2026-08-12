package com.rusty.aurora.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

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

    override fun openPhotoThumbnail(id: String, maxDimension: Int): PhotoStream? {
        val entry = loadEntries().firstOrNull { it.id == id } ?: return null
        return runCatching {
            val uri = Uri.parse(entry.uriString)

            // Decode bounds only first so downsampling happens during decode
            // (inSampleSize), not after a full-resolution decode - the whole
            // point is to never hold a multi-megapixel bitmap in memory.
            // decodeStream() always returns null in inJustDecodeBounds mode
            // by design (it only fills in outWidth/outHeight) - the null
            // check here has to be on the stream itself, not this result,
            // or every thumbnail request "fails" via a spurious non-local
            // return before the real decode even runs.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= maxDimension &&
                bounds.outHeight / (sampleSize * 2) >= maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeStream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val bitmap = decodeStream.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: return@runCatching null

            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val thumbnail = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val out = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
            PhotoStream(out.toByteArray(), "image/jpeg")
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
