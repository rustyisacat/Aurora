package com.rusty.aurora.notifications

import kotlinx.serialization.Serializable

/** Notification count for one app - no titles/text, just "how many". */
@Serializable
data class NotificationGroup(
    val app: String,
    val count: Int
)
