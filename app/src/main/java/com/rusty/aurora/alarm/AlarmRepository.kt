package com.rusty.aurora.alarm

/**
 * Reads the next alarm that will actually wake the user, whichever of two
 * independent sources fires first: the phone's own stock Clock app, or one
 * of Aurora's own wake alarms (see WakeAlarmRepository). Returns null if
 * neither has one set.
 */
interface AlarmRepository {
    fun getNextAlarm(): NextAlarm?

    /**
     * The same alarm as [getNextAlarm], as a raw epoch-millis trigger time
     * rather than a formatted string. Used by SleepTimer's "until alarm"
     * option, which needs to compute a duration, not just display a time -
     * re-deriving that from the "HH:mm" string would mean re-solving the
     * "is this today or tomorrow" ambiguity already resolved once here.
     */
    fun getNextAlarmTriggerMillis(): Long?
}
