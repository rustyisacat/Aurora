package com.rusty.aurora.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.rusty.aurora.AuroraApplication

/**
 * Android instantiates this itself once the user grants notification
 * access in system Settings, so it can't take constructor-injected
 * dependencies like the rest of the app. It reaches into
 * [AuroraApplication]'s container instead - the one deliberate exception
 * to constructor injection, forced by the platform's component model.
 *
 * Only the notification *count* is read here for v0.1 - titles, text, and
 * package names are never touched.
 */
class AuroraNotificationListenerService : NotificationListenerService() {

    private val repository: NotificationCountRepository
        get() = (application as AuroraApplication).container.notificationCountRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateCount()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateCount()
    }

    private fun updateCount() {
        // Recomputed from activeNotifications rather than incremented/decremented,
        // so the count can't drift out of sync from a missed callback.
        repository.setCount(activeNotifications?.size ?: 0)
    }
}
