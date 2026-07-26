package com.rusty.aurora.alarm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure conversion from an alarm trigger time to the API shape, split out
 * from [AlarmRepositoryImpl] so it's unit-testable without needing to
 * construct a real (final, hard-to-fake) AlarmManager.AlarmClockInfo.
 */
internal object AlarmMapper {

    fun toNextAlarm(triggerTimeMillis: Long?): NextAlarm? {
        if (triggerTimeMillis == null) return null

        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return NextAlarm(
            time = formatter.format(Date(triggerTimeMillis)),
            // getNextAlarmClock() only ever reports alarms that are actively
            // armed - there's no "next alarm, but disabled" surfaced through
            // this API - so enabled is always true for a non-null result.
            enabled = true
        )
    }
}
