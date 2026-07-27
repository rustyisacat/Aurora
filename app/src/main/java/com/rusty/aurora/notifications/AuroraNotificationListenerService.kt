package com.rusty.aurora.notifications

import android.content.pm.PackageManager
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
 * Only counts grouped by app are read here - titles and message text are
 * never touched.
 */
class AuroraNotificationListenerService : NotificationListenerService() {

    private val repository: NotificationCountRepository
        get() = (application as AuroraApplication).container.notificationCountRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateGroups()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateGroups()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateGroups()
    }

    private fun updateGroups() {
        // Recomputed from activeNotifications rather than incremented/decremented,
        // so counts can't drift out of sync from a missed callback.
        val packageNames = activeNotifications?.map { it.packageName } ?: emptyList()
        repository.update(NotificationGrouper.group(packageNames, ::resolveAppLabel))
    }

    /**
     * Falls back to the raw package name on any resolution failure - API 30+
     * package-visibility filtering could plausibly hide some apps' labels
     * from us even though we already have their StatusBarNotification (an
     * untested edge case), and a degraded label beats crashing the listener.
     */
    private fun resolveAppLabel(packageName: String): String =
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        } catch (e: SecurityException) {
            packageName
        }
}
