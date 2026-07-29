package com.rusty.aurora.wakealarm

import kotlinx.serialization.Serializable

/** Whether one of Aurora's own alarms is currently ringing, and which -
 *  the Echo Show dashboard polls this to know when to start blasting the
 *  alarm sound and show its dismiss/snooze UI. */
@Serializable
data class WakeAlarmRingingState(
    val ringing: Boolean = false,
    val alarmId: String? = null,
    val label: String = "",
    val soundId: String? = null
)
