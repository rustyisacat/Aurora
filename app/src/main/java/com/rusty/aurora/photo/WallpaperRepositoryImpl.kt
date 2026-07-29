package com.rusty.aurora.photo

import android.content.Context
import android.net.Uri

/** SharedPreferences-backed, same idea as PhotoRepositoryImpl but a single
 *  uri instead of a list - a wallpaper is one image, not a rotation. */
class WallpaperRepositoryImpl(private val context: Context) : WallpaperRepository {

    override fun getWallpaper(): PhotoStream? {
        val uriString = prefs.getString(KEY_WALLPAPER_URI, null) ?: return null
        return runCatching {
            val uri = Uri.parse(uriString)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            PhotoStream(bytes, mimeType)
        }.getOrNull()
    }

    override fun setWallpaper(uri: Uri) {
        prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_NAME = "aurora_wallpaper"
        const val KEY_WALLPAPER_URI = "wallpaper_uri"
    }
}
