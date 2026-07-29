package com.rusty.aurora.api

import com.rusty.aurora.notifications.AppIconProvider
import com.rusty.aurora.notifications.NotificationCountRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.ByteArrayInputStream

/** Dismisses every notification on the phone, the same as clearing them
 *  from the notification shade - see NotificationCountRepository's
 *  clearAllAction for why this has to be requested rather than done
 *  directly (only a live NotificationListenerService instance can). */
class ClearNotificationsRoute(private val notificationCountRepository: NotificationCountRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/notifications/clear"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        notificationCountRepository.clearAll()
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", "OK")
    }
}

/** An installed app's launcher icon, keyed by package name - same
 *  streaming/caching shape as PhotoStreamRoute. */
class NotificationIconRoute(private val appIconProvider: AppIconProvider) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/notifications/icon"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val packageName = session.parameters["package"]?.firstOrNull()
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'package'")

        val bytes = appIconProvider.getIconPng(packageName)
            ?: return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Unknown package: $packageName")

        val response = NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "image/png",
            ByteArrayInputStream(bytes),
            bytes.size.toLong()
        )
        // An app's icon almost never changes between requests - safe for the
        // browser to cache rather than re-fetching every poll cycle.
        response.addHeader("Cache-Control", "public, max-age=86400")
        return response
    }
}
