package com.rusty.aurora.api

import com.rusty.aurora.notifications.NotificationCountRepository
import fi.iki.elonen.NanoHTTPD

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
