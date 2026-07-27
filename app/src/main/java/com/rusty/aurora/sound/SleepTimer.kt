package com.rusty.aurora.sound

import com.rusty.aurora.alarm.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface SleepTimerRequest {
    data object Off : SleepTimerRequest
    data class Fixed(val minutes: Int) : SleepTimerRequest
    data object UntilAlarm : SleepTimerRequest
}

/**
 * Pure timer math, decoupled from coroutines/AlarmManager so it's a plain
 * JVM unit test: parsing the API's preset string, resolving it to a
 * concrete end time given "now" and (for UntilAlarm) the alarm's trigger
 * time, and computing minutes remaining.
 */
internal object SleepTimerCalculator {

    fun parsePreset(raw: String): SleepTimerRequest = when (raw.trim().lowercase()) {
        "off", "" -> SleepTimerRequest.Off
        "untilalarm", "until_alarm", "until-alarm" -> SleepTimerRequest.UntilAlarm
        else -> raw.toIntOrNull()?.let { SleepTimerRequest.Fixed(it) } ?: SleepTimerRequest.Off
    }

    /**
     * Null means "no timer to run": either explicitly Off, or UntilAlarm
     * with no alarm currently scheduled (or one that's already passed).
     */
    fun resolveEndTimeMillis(
        request: SleepTimerRequest,
        nowMillis: Long,
        nextAlarmTriggerMillis: Long?
    ): Long? = when (request) {
        is SleepTimerRequest.Off -> null
        is SleepTimerRequest.Fixed -> nowMillis + request.minutes * 60_000L
        is SleepTimerRequest.UntilAlarm -> nextAlarmTriggerMillis?.takeIf { it > nowMillis }
    }

    fun minutesRemaining(endTimeMillis: Long?, nowMillis: Long): Int? =
        endTimeMillis?.let { ((it - nowMillis) / 60_000L).toInt().coerceAtLeast(0) }
}

/**
 * Tracks the countdown and resolves "until alarm" against Aurora's own
 * alarm data (the Echo Show's browser has no way to know the phone's next
 * alarm itself). The actual fade-out is executed client-side, in the
 * browser, against its own Web Audio GainNode - polling can't drive a
 * precise 10-second ramp - so [onExpired] here only needs to flip Aurora's
 * tracked state to "stopped" once the timer reaches zero, keeping
 * /dashboard consistent with what the browser is expected to have done.
 */
class SleepTimer(
    private val alarmRepository: AlarmRepository,
    private val onExpired: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var endTimeMillis: Long? = null

    val minutesRemaining: Int?
        get() = SleepTimerCalculator.minutesRemaining(endTimeMillis, clock())

    /** [rawPreset] is one of "off", "15", "30", "45", "60", "120", "untilAlarm". */
    fun start(rawPreset: String) {
        cancel()
        val request = SleepTimerCalculator.parsePreset(rawPreset)
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            request = request,
            nowMillis = clock(),
            nextAlarmTriggerMillis = alarmRepository.getNextAlarmTriggerMillis()
        ) ?: return

        endTimeMillis = end
        scheduleTick()
    }

    fun cancel() {
        tickJob?.cancel()
        tickJob = null
        endTimeMillis = null
    }

    private fun scheduleTick() {
        tickJob = scope.launch {
            while (true) {
                val end = endTimeMillis ?: return@launch
                val remainingMs = end - clock()
                if (remainingMs <= 0) {
                    endTimeMillis = null
                    onExpired()
                    return@launch
                }
                delay(minOf(remainingMs, TICK_INTERVAL_MS))
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 15_000L
    }
}
