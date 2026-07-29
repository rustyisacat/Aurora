package com.rusty.aurora.photo

/** Which mode the dashboard's main-screen wallpaper is in, and the
 *  settings each mode needs - kept separate from [PhotoRepository] since
 *  that's just the shared pool of available photos, not which of them (or
 *  in what pattern) gets shown. */
interface WallpaperConfigRepository {
    fun getMode(): WallpaperMode
    fun setMode(mode: WallpaperMode)

    /** Only meaningful in SINGLE mode - null if never set, or if the
     *  photo it pointed at was removed from the library. */
    fun getSinglePhotoId(): String?
    fun setSinglePhotoId(photoId: String?)

    /** Only meaningful in SCHEDULED mode - empty until the user adds at
     *  least one entry from the phone app. */
    fun getSchedule(): List<WallpaperScheduleEntry>
    fun setSchedule(entries: List<WallpaperScheduleEntry>)
}
