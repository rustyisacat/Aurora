package com.rusty.aurora.alarm

import kotlinx.serialization.Serializable

@Serializable
data class NextAlarm(
    val time: String,
    val enabled: Boolean
)
