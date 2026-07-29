package com.rusty.aurora.photo

import android.net.Uri

interface WallpaperRepository {
    /** Null until the user picks one via the Photo Picker (see
     *  MainActivity's wallpaperPickerLauncher). The dashboard shows no
     *  wallpaper layer at all - just its normal weather background -
     *  until this is set. */
    fun getWallpaper(): PhotoStream?

    /** Replaces the current wallpaper, if any. [uri] must already have a
     *  persisted (takePersistableUriPermission'd) read grant. */
    fun setWallpaper(uri: Uri)
}
