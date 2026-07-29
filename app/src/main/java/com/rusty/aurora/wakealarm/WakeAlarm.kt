package com.rusty.aurora.wakealarm

import kotlinx.serialization.Serializable

/**
 * A user-configured alarm, scheduled and fired entirely within Aurora
 * rather than the phone's own stock Clock app - see WakeAlarmRepository's
 * doc comment for why.
 */
@Serializable
data class WakeAlarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    // java.util.Calendar.SUNDAY(1)..SATURDAY(7). Empty means "just once" -
    // fires at the next occurrence of hour:minute, then disables itself,
    // same as leaving every day unchecked in a normal clock app.
    val daysOfWeek: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val label: String = "",
    // Null defers to the sound library's own default (see SoundRepository.play).
    val soundId: String? = null
)
