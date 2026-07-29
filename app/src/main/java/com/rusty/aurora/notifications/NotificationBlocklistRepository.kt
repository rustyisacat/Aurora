package com.rusty.aurora.notifications

import kotlinx.coroutines.flow.StateFlow

/** One app that has posted at least one notification since this repository
 *  started tracking it - stays listed (so it stays toggleable) even after
 *  its notifications are all cleared or it gets blocked. */
data class KnownApp(val packageName: String, val label: String)

/**
 * Which apps' notifications are excluded from the dashboard, and every app
 * seen well enough to offer a toggle for - kept separate from the live
 * notification count/groups (see NotificationCountRepository) since this
 * is persisted configuration, not transient state.
 */
interface NotificationBlocklistRepository {
    val blockedPackages: StateFlow<Set<String>>
    val knownApps: StateFlow<List<KnownApp>>

    /** No-op if [packageName] is already known - cheap to call on every
     *  notification event without worrying about redundant writes. */
    fun recordSeen(packageName: String, label: String)

    fun setBlocked(packageName: String, blocked: Boolean)
}
