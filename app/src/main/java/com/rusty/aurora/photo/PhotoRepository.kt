package com.rusty.aurora.photo

import android.net.Uri

interface PhotoRepository {
    /** Ambient Mode's photo background - empty until the user picks some
     *  via the Photo Picker (see MainActivity's photoPickerLauncher). The
     *  dashboard falls back to a procedural starfield when this is empty. */
    fun getPhotos(): List<PhotoInfo>

    /** Replaces the whole selection. Each uri must already have a
     *  persisted (takePersistableUriPermission'd) read grant, since this
     *  list needs to keep working across app/service restarts. */
    fun setPhotos(uris: List<Uri>)

    /** Null if [id] isn't in the current selection, or its uri's grant was
     *  revoked (e.g. the photo was deleted from the phone). */
    fun openPhotoStream(id: String): PhotoStream?
}

data class PhotoInfo(val id: String, val uriString: String)

/** [mimeType] e.g. "image/jpeg"; [bytes] the whole file - a handful of
 *  personal photos, same "just read it all into memory" approach
 *  SoundStream uses for short audio clips. */
data class PhotoStream(val bytes: ByteArray, val mimeType: String)
