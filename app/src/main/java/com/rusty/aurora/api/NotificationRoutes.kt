package com.rusty.aurora.api

import com.rusty.aurora.notifications.AppIconProvider
import com.rusty.aurora.notifications.NotificationBlocklistRepository
import com.rusty.aurora.notifications.NotificationCountRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

private val notificationJson = Json { encodeDefaults = true }

private fun NanoHTTPD.IHTTPSession.singleParam(name: String): String? =
    parameters[name]?.firstOrNull()

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

@Serializable
private data class KnownAppEntry(val packageName: String, val label: String, val blocked: Boolean)

/** Every app that's ever posted a notification, plus its current blocked
 *  state - the dashboard-side equivalent of the phone app's
 *  NotificationAppsCard. Read separately from /dashboard rather than
 *  folded into DashboardResponse since it only matters on the Settings
 *  page, not every 30s poll. */
class KnownNotificationAppsRoute(
    private val notificationBlocklistRepository: NotificationBlocklistRepository
) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/notifications/apps"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val blocked = notificationBlocklistRepository.blockedPackages.value
        val entries = notificationBlocklistRepository.knownApps.value.map {
            KnownAppEntry(it.packageName, it.label, blocked = it.packageName in blocked)
        }
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            notificationJson.encodeToString(entries)
        )
    }
}

/** Mirrors AuroraViewModel's onToggleAppBlocked handler exactly - setBlocked()
 *  alone doesn't re-filter the live notification list, refresh() is what
 *  makes a newly-blocked app's notifications actually disappear (or a
 *  newly-unblocked one's reappear) without waiting for its next post. */
class SetNotificationBlockedRoute(
    private val notificationBlocklistRepository: NotificationBlocklistRepository,
    private val notificationCountRepository: NotificationCountRepository
) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/notifications/block"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val packageName = session.singleParam("package")
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'package'")
        val blocked = session.singleParam("blocked")?.toBooleanStrictOrNull()
            ?: return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'blocked' (expected true/false)"
            )
        notificationBlocklistRepository.setBlocked(packageName, blocked)
        notificationCountRepository.refresh()
        return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")
    }
}
