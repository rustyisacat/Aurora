package com.rusty.aurora.wakealarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextTriggerCalculatorTest {

    private fun at(hour: Int, minute: Int, second: Int = 0): Calendar =
        Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 27, hour, minute, second) // a Monday
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `one-time alarm later today fires today`() {
        val from = at(10, 0)
        val result = NextTriggerCalculator.nextTriggerMillis(
            hour = 14, minute = 30, daysOfWeek = emptySet(), fromMillis = from.timeInMillis
        )

        val expected = at(14, 30)
        assertEquals(expected.timeInMillis, result)
    }

    @Test
    fun `one-time alarm already passed today fires tomorrow`() {
        val from = at(10, 0)
        val result = NextTriggerCalculator.nextTriggerMillis(
            hour = 6, minute = 0, daysOfWeek = emptySet(), fromMillis = from.timeInMillis
        )

        val expected = (at(6, 0).clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expected.timeInMillis, result)
    }

    @Test
    fun `repeating alarm fires today when today matches and time hasn't passed`() {
        val from = at(10, 0)
        val today = from.get(Calendar.DAY_OF_WEEK)

        val result = NextTriggerCalculator.nextTriggerMillis(
            hour = 14, minute = 0, daysOfWeek = setOf(today), fromMillis = from.timeInMillis
        )

        assertEquals(at(14, 0).timeInMillis, result)
    }

    @Test
    fun `repeating alarm skips to next week when today matches but time already passed`() {
        val from = at(10, 0)
        val today = from.get(Calendar.DAY_OF_WEEK)

        val result = NextTriggerCalculator.nextTriggerMillis(
            hour = 6, minute = 0, daysOfWeek = setOf(today), fromMillis = from.timeInMillis
        )

        val expected = (at(6, 0).clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
        assertEquals(expected.timeInMillis, result)
    }

    @Test
    fun `repeating alarm picks the next matching weekday when today doesn't match`() {
        val from = at(10, 0)
        val today = from.get(Calendar.DAY_OF_WEEK)
        val threeDaysLater = ((today - 1 + 3) % 7) + 1 // Calendar.DAY_OF_WEEK is 1-based

        val result = NextTriggerCalculator.nextTriggerMillis(
            hour = 6, minute = 0, daysOfWeek = setOf(threeDaysLater), fromMillis = from.timeInMillis
        )

        val expected = (at(6, 0).clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3) }
        assertEquals(expected.timeInMillis, result)
    }
}
