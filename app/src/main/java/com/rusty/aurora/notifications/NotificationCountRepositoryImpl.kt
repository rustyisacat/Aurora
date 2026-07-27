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
}
