package com.rusty.aurora.api

import com.rusty.aurora.layout.LayoutRepository
import com.rusty.aurora.layout.TileConfig
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val layoutJson = Json { encodeDefaults = true }

/** A JSON body, not query params - a list of {id, visible, size} tiles
 *  doesn't fit a query string cleanly. Same deliberate, scoped exception
 *  as PhotoRoutes.kt's /photos/wallpaper/schedule. */
private fun NanoHTTPD.IHTTPSession.readJsonBody(): String? {
    val files = HashMap<String, String>()
    parseBody(files)
    return files["postData"]
}

/** Current layout is already returned via /dashboard's `layout` field - no
 *  separate GET route needed, only the write side is new. Reuses
 *  LayoutRepository.setLayout()'s existing "reject if it'd leave zero
 *  visible tiles" guard rather than re-validating here. */
class SetLayoutRoute(private val layoutRepository: LayoutRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/layout"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = session.readJsonBody()
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing JSON body")
        val tiles = try {
            layoutJson.decodeFromString<List<TileConfig>>(body)
        } catch (e: Exception) {
            return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Invalid JSON: ${e.message}")
        }
        layoutRepository.setLayout(tiles)
        return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")
    }
}
