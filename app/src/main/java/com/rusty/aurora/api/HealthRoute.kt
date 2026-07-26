package com.rusty.aurora.api

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status

class HealthRoute : Route {

    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/health"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "Aurora OK")
}
