package com.rusty.aurora.photo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How the dashboard picks which photo (from the shared library - see
 *  PhotoRepository) to show as its main-screen wallpaper. */
@Serializable
enum class WallpaperMode {
    @SerialName("rotating") ROTATING,
    @SerialName("single") SINGLE,
    @SerialName("scheduled") SCHEDULED
}

/** One scheduled change: show [photoId] starting at [time] ("HH:mm", 24h,
 *  local time). The dashboard picks whichever entry's time has most
 *  recently passed, wrapping around midnight - entries always cover the
 *  full day with no gaps or overlaps to configure by hand. */
@Serializable
data class WallpaperScheduleEntry(val photoId: String, val time: String)
