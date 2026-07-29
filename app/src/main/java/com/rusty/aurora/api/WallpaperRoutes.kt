package com.rusty.aurora.api

import com.rusty.aurora.photo.WallpaperRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.ByteArrayInputStream

/**
 * The dashboard's wallpaper: a single image, unlike Ambient Mode's photo
 * rotation, so no library-listing route - just the bytes themselves, or
 * 404 if nothing's been picked yet.
 */
class WallpaperImageRoute(private val wallpaperRepository: WallpaperRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/wallpaper/image"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val stream = wallpaperRepository.getWallpaper()
            ?: return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "No wallpaper set")

        val response = NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            stream.mimeType,
            ByteArrayInputStream(stream.bytes),
            stream.bytes.size.toLong()
        )
        // Static until the user picks a different one - safe to cache, but
        // short-lived so a newly-picked wallpaper shows up on the next
        // reload rather than being stuck behind a day-long cache.
        response.addHeader("Cache-Control", "public, max-age=300")
        return response
    }
}
