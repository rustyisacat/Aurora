package com.rusty.aurora.wakealarm

import java.util.Calendar

/**
 * Pure "when does this alarm next fire" math, kept separate from
 * WakeAlarmScheduler so it's a plain JVM unit test - the same
 * testable-core-vs-Android-glue split used elsewhere (e.g.
 * SleepTimerCalculator, CalendarEventMapper).
 */
internal object NextTriggerCalculator {

    /**
     * Next occurrence of [hour]:[minute] strictly after [fromMillis].
     * Empty [daysOfWeek] means "just once" - the very next occurrence,
     * today if it hasn't passed yet, otherwise tomorrow. Non-empty means
     * "next matching weekday" (java.util.Calendar.SUNDAY(1)..SATURDAY(7)),
     * which may still be today if that day qualifies and the time hasn't
     * passed yet.
     */
    fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        fromMillis: Long
    ): Long {
        val candidate = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            if (candidate.timeInMillis <= fromMillis) candidate.add(Calendar.DAY_OF_YEAR, 1)
            return candidate.timeInMillis
        }

        repeat(8) { offset ->
            val trial = (candidate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            if (trial.get(Calendar.DAY_OF_WEEK) in daysOfWeek && trial.timeInMillis > fromMillis) {
                return trial.timeInMillis
            }
        }
        error("daysOfWeek must contain at least one valid Calendar.DAY_OF_WEEK value")
    }
}
