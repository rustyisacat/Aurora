package com.rusty.aurora.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationCountRepositoryImpl : NotificationCountRepository {

    private val _notificationCount = MutableStateFlow(0)
    override val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    override fun setCount(count: Int) {
        _notificationCount.value = count
    }
}
