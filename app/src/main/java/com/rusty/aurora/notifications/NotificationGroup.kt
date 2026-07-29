package com.rusty.aurora.notifications

import kotlinx.serialization.Serializable

/** One app's notifications, grouped: how many, plus a preview of the most
 *  recent one (by postTime) so the dashboard can show more than a bare
 *  count. packageName drives both icon lookup (/notifications/icon) and
 *  the phone-side blocklist. */
@Serializable
data class NotificationGroup(
    val app: String,
    val packageName: String,
    val count: Int,
    val latestTitle: String = "",
    val latestText: String = ""
)
