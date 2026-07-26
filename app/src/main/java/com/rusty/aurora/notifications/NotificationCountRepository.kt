package com.rusty.aurora.notifications

import kotlinx.coroutines.flow.StateFlow

/**
 * Live count of currently active notifications.
 *
 * Exposed as a [StateFlow] rather than a plain getter because the count
 * changes asynchronously, pushed by [AuroraNotificationListenerService]
 * callbacks rather than pulled by the HTTP server or UI.
 */
interface NotificationCountRepository {
    val notificationCount: StateFlow<Int>
    fun setCount(count: Int)
}
