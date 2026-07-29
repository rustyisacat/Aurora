package com.rusty.aurora.wakealarm

/**
 * Aurora's own alarm clock, scheduled and fired entirely on the phone
 * rather than by reading/silencing the phone's stock Clock app. That
 * turned out to be the only reliable option: Android has no general API
 * for a third-party app to enumerate another app's set alarms (the stock
 * Clock's alarm list lives in that app's own, often OEM-specific, private
 * database), let alone silence one mid-ring. Building the alarm here
 * sidesteps all of that - Aurora never needs to make noise on the phone
 * at all, since the Echo Show is what actually rings (see the "ringing
 * state" plumbing below, which mirrors SoundRepository's "Aurora tracks
 * desired state, the browser does playback" split).
 */
interface WakeAlarmRepository {
    fun getAlarms(): List<WakeAlarm>

    /** Upserts by [WakeAlarm.id] - same id twice replaces and reschedules. */
    fun setAlarm(alarm: WakeAlarm)

    fun deleteAlarm(id: String)

    fun getRingingState(): WakeAlarmRingingState

    /** Called by WakeAlarmReceiver when AlarmManager actually fires. */
    fun handleFired(alarmId: String)

    fun dismiss()

    /** Re-fires the currently-ringing alarm in [minutes]; a no-op if nothing's ringing. */
    fun snooze(minutes: Int)

    /** Re-arms every enabled alarm with AlarmManager - alarms don't survive
     *  a reboot on their own, so this is called from WakeAlarmBootReceiver. */
    fun rearmAll()

    /** Earliest upcoming trigger among enabled alarms, or null if none -
     *  folded into AlarmRepository.getNextAlarmTriggerMillis() so the sound
     *  machine's "until alarm" sleep timer preset stays meaningful
     *  regardless of which alarm system the user actually wakes up to. */
    fun getEarliestEnabledTriggerMillis(): Long?
}
