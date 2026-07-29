package com.rusty.aurora.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationCountRepositoryImpl : NotificationCountRepository {

    private val _notificationCount = MutableStateFlow(0)
    override val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    private val _notificationGroups = MutableStateFlow(emptyList<NotificationGroup>())
    override val notificationGroups: StateFlow<List<NotificationGroup>> = _notificationGroups.asStateFlow()

    override fun update(groups: List<NotificationGroup>) {
        _notificationGroups.value = groups
        _notificationCount.value = groups.sumOf { it.count }
    }

    @Volatile
    private var clearAllAction: (() -> Unit)? = null

    override fun setClearAllAction(action: (() -> Unit)?) {
        clearAllAction = action
    }

    override fun clearAll() {
        clearAllAction?.invoke()
    }

    @Volatile
    private var refreshAction: (() -> Unit)? = null

    override fun setRefreshAction(action: (() -> Unit)?) {
        refreshAction = action
    }

    override fun refresh() {
        refreshAction?.invoke()
    }
}
