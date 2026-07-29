package com.rusty.aurora.api

import com.rusty.aurora.photo.PhotoRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.Serializable
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
 */

private val photoJson = Json { encodeDefaults = true }

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
