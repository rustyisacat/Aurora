package com.rusty.aurora.api

import fi.iki.elonen.NanoHTTPD

/**
 * One HTTP endpoint. Adding a new field to the dashboard (weather,
 * calendar, ...) means writing a new [Route] implementation and listing
 * it alongside the others in [com.rusty.aurora.di.AppContainer] -
 * [AuroraHttpServer] itself never has to change.
 */
interface Route {
    fun matches(session: NanoHTTPD.IHTTPSession): Boolean
    fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response
}
