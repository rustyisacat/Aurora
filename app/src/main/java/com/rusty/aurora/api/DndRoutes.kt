package com.rusty.aurora.api

import com.rusty.aurora.notifications.DndRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status

/** POST /dnd/set?enabled=true|false - same query-string-param shape as the
 *  sound machine's control routes. Current state is read as part of the
 *  regular /dashboard poll (DashboardResponse.dndEnabled), not a separate
 *  GET route - the dashboard doesn't need it any more often than that. */
class SetDndRoute(private val dndRepository: DndRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/dnd/set"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val enabled = session.parameters["enabled"]?.firstOrNull()?.toBooleanStrictOrNull()
            ?: return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'enabled' (expected true/false)"
            )
        dndRepository.setEnabled(enabled)
        return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")
    }
}
