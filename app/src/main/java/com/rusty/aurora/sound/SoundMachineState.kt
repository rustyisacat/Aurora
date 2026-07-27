package com.rusty.aurora.sound

import kotlinx.serialization.Serializable

/**
 * Sound machine state as exposed on /dashboard. Unlike nextAlarm/weather,
 * this is never null - "not playing, nothing loaded" is a well-defined
 * state, not an absence of data. sleepTimerMinutes is minutes *remaining*,
 * not the originally-selected duration - more useful for a glance display.
 */
@Serializable
data class SoundMachineState(
    val playing: Boolean,
    val sound: String?,
    val volume: Int,
    val sleepTimerMinutes: Int?
)
