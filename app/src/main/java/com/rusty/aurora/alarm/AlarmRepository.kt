package com.rusty.aurora.alarm

/** Reads the phone's next scheduled alarm. Returns null if none is set. */
interface AlarmRepository {
    fun getNextAlarm(): NextAlarm?
}
