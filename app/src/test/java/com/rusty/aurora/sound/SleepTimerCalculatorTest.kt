package com.rusty.aurora.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepTimerCalculatorTest {

    @Test
    fun `parses fixed presets`() {
        assertEquals(SleepTimerRequest.Fixed(15), SleepTimerCalculator.parsePreset("15"))
        assertEquals(SleepTimerRequest.Fixed(120), SleepTimerCalculator.parsePreset("120"))
    }

    @Test
    fun `parses off, empty, and unrecognized input as Off`() {
        assertEquals(SleepTimerRequest.Off, SleepTimerCalculator.parsePreset("off"))
        assertEquals(SleepTimerRequest.Off, SleepTimerCalculator.parsePreset("OFF"))
        assertEquals(SleepTimerRequest.Off, SleepTimerCalculator.parsePreset(""))
        assertEquals(SleepTimerRequest.Off, SleepTimerCalculator.parsePreset("nonsense"))
    }

    @Test
    fun `parses untilAlarm case-insensitively`() {
        assertEquals(SleepTimerRequest.UntilAlarm, SleepTimerCalculator.parsePreset("untilAlarm"))
        assertEquals(SleepTimerRequest.UntilAlarm, SleepTimerCalculator.parsePreset("UNTILALARM"))
        assertEquals(SleepTimerRequest.UntilAlarm, SleepTimerCalculator.parsePreset("until_alarm"))
    }

    @Test
    fun `Off never resolves to an end time`() {
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            SleepTimerRequest.Off, nowMillis = 1_000L, nextAlarmTriggerMillis = 5_000L
        )
        assertNull(end)
    }

    @Test
    fun `Fixed resolves to now plus the given minutes`() {
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            SleepTimerRequest.Fixed(30), nowMillis = 1_000L, nextAlarmTriggerMillis = null
        )
        assertEquals(1_000L + 30 * 60_000L, end)
    }

    @Test
    fun `UntilAlarm resolves to the alarm's trigger time when it's in the future`() {
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            SleepTimerRequest.UntilAlarm, nowMillis = 1_000L, nextAlarmTriggerMillis = 50_000L
        )
        assertEquals(50_000L, end)
    }

    @Test
    fun `UntilAlarm resolves to null when there is no alarm`() {
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            SleepTimerRequest.UntilAlarm, nowMillis = 1_000L, nextAlarmTriggerMillis = null
        )
        assertNull(end)
    }

    @Test
    fun `UntilAlarm resolves to null when the alarm has already passed`() {
        val end = SleepTimerCalculator.resolveEndTimeMillis(
            SleepTimerRequest.UntilAlarm, nowMillis = 10_000L, nextAlarmTriggerMillis = 5_000L
        )
        assertNull(end)
    }

    @Test
    fun `minutesRemaining rounds down and never goes negative`() {
        assertEquals(5, SleepTimerCalculator.minutesRemaining(endTimeMillis = 300_000L, nowMillis = 0L))
        assertEquals(0, SleepTimerCalculator.minutesRemaining(endTimeMillis = 10_000L, nowMillis = 15_000L))
        assertNull(SleepTimerCalculator.minutesRemaining(endTimeMillis = null, nowMillis = 0L))
    }
}
