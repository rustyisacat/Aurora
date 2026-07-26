package com.rusty.aurora.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status

/**
 * Thin NanoHTTPD subclass: dispatch to whichever [Route] matches the
 * request, or 404 otherwise. All endpoint logic lives in the [Route]
 * implementations, not here, so this class stays stable as routes are
 * added.
 */
class AuroraHttpServer(
    port: Int,
    private val routes: List<Route>
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val route = routes.firstOrNull { it.matches(session) }
            ?: return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not found")

        return route.handle(session)
    }

    companion object {
        const val DEFAULT_SOCKET_TIMEOUT_MS = 5000
    }
}
