package com.rusty.aurora.notifications

import kotlinx.coroutines.flow.StateFlow

/**
 * Live notification state, exposed as [StateFlow]s rather than plain getters
 * because it changes asynchronously, pushed by [AuroraNotificationListenerService]
 * callbacks rather than pulled by the HTTP server or UI.
 *
 * [notificationCount] and [notificationGroups] are always updated together
 * via [update] - count is derived as the sum of the groups, so the two can
 * never drift out of sync with each other.
 */
interface NotificationCountRepository {
    val notificationCount: StateFlow<Int>
    val notificationGroups: StateFlow<List<NotificationGroup>>
    fun update(groups: List<NotificationGroup>)

    /**
     * Registers the action that actually dismisses notifications - only
     * a live, connected NotificationListenerService instance can call
     * cancelAllNotifications(), so AuroraNotificationListenerService
     * registers itself here on connect and clears it on disconnect. Null
     * means "not currently connected."
     */
    fun setClearAllAction(action: (() -> Unit)?)

    /** Requests that every notification be dismissed on the phone - a
     *  no-op if the listener isn't currently connected. */
    fun clearAll()
}
