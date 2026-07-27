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
}
