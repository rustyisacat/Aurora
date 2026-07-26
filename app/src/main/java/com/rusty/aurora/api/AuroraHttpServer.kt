package com.rusty.aurora.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status

/**
 * Thin NanoHTTPD subclass: dispatch to whichever [Route] matches the
 * request, or 404 otherwise. All endpoint logic lives in the [Route]
 * implementations, not here, so this class stays stable as routes are
 * added.
 *
 * CORS headers are applied here, not per-route, since browser cross-origin
 * rules are an HTTP-layer concern, not a route concern - the client that
 * matters (the Echo Show dashboard) is a browser fetch() from a different
 * origin than Aurora itself. Without these, the response is delivered
 * correctly but the browser silently refuses to let JS read it.
 */
class AuroraHttpServer(
    port: Int,
    private val routes: List<Route>
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        // Preflight: browsers send this before certain cross-origin requests,
        // and Chrome also sends it for any request to a private-network
        // address like Aurora's LAN IP (Private Network Access). No route
        // needs to see it - answer it directly here.
        if (session.method == Method.OPTIONS) {
            return withCorsHeaders(newFixedLengthResponse(Status.OK, "text/plain", ""))
        }

        val route = routes.firstOrNull { it.matches(session) }
            ?: return withCorsHeaders(newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not found"))

        return withCorsHeaders(route.handle(session))
    }

    private fun withCorsHeaders(response: Response): Response {
        // Aurora has no auth and is only ever reachable on the home LAN, so
        // a wildcard origin is fine - simpler than trying to predict how the
        // dashboard's static files end up being served (file://, a local
        // static server, ...) and it may change.
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type")
        response.addHeader("Access-Control-Allow-Private-Network", "true")
        return response
    }

    companion object {
        const val DEFAULT_SOCKET_TIMEOUT_MS = 5000
    }
}
