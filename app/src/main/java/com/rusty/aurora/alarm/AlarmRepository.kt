package com.rusty.aurora.alarm

/** Reads the phone's next scheduled alarm. Returns null if none is set. */
interface AlarmRepository {
    fun getNextAlarm(): NextAlarm?

    /**
     * The same alarm as [getNextAlarm], as a raw epoch-millis trigger time
     * rather than a formatted string. Used by SleepTimer's "until alarm"
     * option, which needs to compute a duration, not just display a time -
     * re-deriving that from the "HH:mm" string would mean re-solving the
     * "is this today or tomorrow" ambiguity AlarmManager already resolved.
     */
    fun getNextAlarmTriggerMillis(): Long?
}
