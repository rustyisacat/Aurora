package com.rusty.aurora.notifications

import android.app.Notification
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
 * Reads each notification's title/text (via [NotificationBlocklistRepository]
 * this is opt-out per app, not read unconditionally forever) so the
 * dashboard can show more than a bare count.
 */
class AuroraNotificationListenerService : NotificationListenerService() {

    private val repository: NotificationCountRepository
        get() = (application as AuroraApplication).container.notificationCountRepository

    private val blocklistRepository: NotificationBlocklistRepository
        get() = (application as AuroraApplication).container.notificationBlocklistRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        repository.setClearAllAction(::cancelAllNotifications)
        repository.setRefreshAction(::updateGroups)
        updateGroups()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        repository.setClearAllAction(null)
        repository.setRefreshAction(null)
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
        val active = activeNotifications ?: emptyArray()

        // Recorded from the full, unfiltered set - a blocked app should stay
        // listed (and toggleable) in the phone app's Notification Apps card,
        // not disappear once it's excluded.
        active.forEach { sbn -> blocklistRepository.recordSeen(sbn.packageName, resolveAppLabel(sbn.packageName)) }

        val blocked = blocklistRepository.blockedPackages.value
        val entries = active
            .filter { it.packageName !in blocked }
            .map { sbn ->
                val extras = sbn.notification.extras
                NotificationGrouper.Entry(
                    packageName = sbn.packageName,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    postTimeMs = sbn.postTime
                )
            }
        repository.update(NotificationGrouper.group(entries, ::resolveAppLabel))
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
