package com.rusty.aurora.api

import com.rusty.aurora.photo.PhotoRepository
import com.rusty.aurora.photo.WallpaperConfigRepository
import com.rusty.aurora.photo.WallpaperMode
import com.rusty.aurora.photo.WallpaperScheduleEntry
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

/**
 * Ambient Mode's photo background: a library listing plus the byte stream
 * itself, same shape as SoundRoutes.kt's SoundLibraryRoute/SoundStreamRoute
 * (this is the second resource to follow that "small library route(s) +
 * byte stream route" pattern, not a new one). Aurora never renders
 * anything itself - the Echo Show's kiosk browser fetches the raw bytes
 * and displays them directly.
 *
 * The wallpaper *write* routes below are new - photos are still only ever
 * imported from the phone (no filesystem access from the dashboard), but
 * once a library exists, which photo(s) to show and in what pattern is
 * pure data the dashboard can drive directly.
 */

private val photoJson = Json { encodeDefaults = true }

private fun okResponse(): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")

private fun NanoHTTPD.IHTTPSession.singleParam(name: String): String? =
    parameters[name]?.firstOrNull()

/** /photos/wallpaper/schedule is the one route in this file with a JSON
 *  body instead of query params - a list of {photoId, time} entries
 *  doesn't fit a query string cleanly, same deliberate exception as
 *  LayoutRoutes.kt's /layout. */
private fun NanoHTTPD.IHTTPSession.readJsonBody(): String? {
    val files = HashMap<String, String>()
    parseBody(files)
    return files["postData"]
}

@Serializable
private data class PhotoLibraryEntry(val id: String)

class PhotoLibraryRoute(private val photoRepository: PhotoRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/photos/library"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val entries = photoRepository.getPhotos().map { PhotoLibraryEntry(it.id) }
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            photoJson.encodeToString(entries)
        )
    }
}

class PhotoStreamRoute(private val photoRepository: PhotoRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/photos/stream"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val id = session.parameters["id"]?.firstOrNull()
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'id'")

        val stream = photoRepository.openPhotoStream(id)
            ?: return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Unknown photo: $id")

        val response = NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            stream.mimeType,
            ByteArrayInputStream(stream.bytes),
            stream.bytes.size.toLong()
        )
        // A photo's id is derived from its uri, so it's static for as long
        // as it stays selected - safe for the browser to cache rather than
        // re-fetching every cycle through the same handful of photos.
        response.addHeader("Cache-Control", "public, max-age=86400")
        return response
    }
}

class SetWallpaperModeRoute(private val wallpaperConfigRepository: WallpaperConfigRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/photos/wallpaper/mode"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val mode = when (session.singleParam("mode")) {
            "rotating" -> WallpaperMode.ROTATING
            "single" -> WallpaperMode.SINGLE
            "scheduled" -> WallpaperMode.SCHEDULED
            else -> return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'mode' (expected rotating/single/scheduled)"
            )
        }
        wallpaperConfigRepository.setMode(mode)
        return okResponse()
    }
}

/** photoId may be omitted/blank to clear the single-photo selection - same
 *  "null clears it" shape as WallpaperConfigRepository.setSinglePhotoId(). */
class SetWallpaperSinglePhotoRoute(private val wallpaperConfigRepository: WallpaperConfigRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/photos/wallpaper/single"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        wallpaperConfigRepository.setSinglePhotoId(session.singleParam("photoId")?.trim()?.takeIf { it.isNotEmpty() })
        return okResponse()
    }
}

class SetWallpaperScheduleRoute(private val wallpaperConfigRepository: WallpaperConfigRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/photos/wallpaper/schedule"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = session.readJsonBody()
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing JSON body")
        val entries = try {
            photoJson.decodeFromString<List<WallpaperScheduleEntry>>(body)
        } catch (e: Exception) {
            return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Invalid JSON: ${e.message}")
        }
        wallpaperConfigRepository.setSchedule(entries)
        return okResponse()
    }
}
