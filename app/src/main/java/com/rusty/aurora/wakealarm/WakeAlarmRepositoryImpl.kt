package com.rusty.aurora.wakealarm

import android.content.Context
import com.rusty.aurora.sound.SoundLibrary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WakeAlarmRepositoryImpl(
    context: Context,
    private val scheduler: WakeAlarmScheduler,
    private val soundLibrary: SoundLibrary
) : WakeAlarmRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var alarms: List<WakeAlarm> = loadAlarms()

    @Volatile
    private var ringingState: WakeAlarmRingingState = loadRingingState()

    @Volatile
    private var defaultAlarmSoundId: String? = prefs.getString(KEY_DEFAULT_ALARM_SOUND, null)

    override fun getAlarms(): List<WakeAlarm> = alarms

    override fun setAlarm(alarm: WakeAlarm) {
        alarms = alarms.filterNot { it.id == alarm.id } + alarm
        persistAlarms()
        scheduler.schedule(alarm)
    }

    override fun deleteAlarm(id: String) {
        alarms = alarms.filterNot { it.id == id }
        persistAlarms()
        scheduler.cancel(id)
    }

    override fun getRingingState(): WakeAlarmRingingState = ringingState

    override fun handleFired(alarmId: String) {
        val alarm = alarms.find { it.id == alarmId } ?: return
        val soundId = alarm.soundId ?: defaultAlarmSoundId ?: soundLibrary.getAll().firstOrNull()?.id
        ringingState = WakeAlarmRingingState(
            ringing = true, alarmId = alarm.id, label = alarm.label, soundId = soundId
        )
        persistRingingState()

        // One-time alarms disable themselves after firing, same as a
        // normal clock app; repeating ones just get rescheduled for their
        // next matching day.
        if (alarm.daysOfWeek.isEmpty()) {
            setAlarm(alarm.copy(enabled = false))
        } else {
            scheduler.schedule(alarm)
        }
    }

    override fun dismiss() {
        ringingState = WakeAlarmRingingState()
        persistRingingState()
    }

    override fun snooze(minutes: Int) {
        val alarm = alarms.find { it.id == ringingState.alarmId }
        ringingState = WakeAlarmRingingState()
        persistRingingState()
        if (alarm != null) scheduler.scheduleSnooze(alarm, minutes)
    }

    override fun rearmAll() {
        alarms.filter { it.enabled }.forEach(scheduler::schedule)
    }

    override fun getEarliestEnabledTriggerMillis(): Long? =
        alarms.filter { it.enabled }
            .map { NextTriggerCalculator.nextTriggerMillis(it.hour, it.minute, it.daysOfWeek, System.currentTimeMillis()) }
            .minOrNull()

    override fun getDefaultAlarmSoundId(): String? = defaultAlarmSoundId

    override fun setDefaultAlarmSoundId(id: String) {
        defaultAlarmSoundId = id
        prefs.edit().putString(KEY_DEFAULT_ALARM_SOUND, id).apply()
    }

    private fun loadAlarms(): List<WakeAlarm> =
        prefs.getString(KEY_ALARMS, null)?.let {
            runCatching { json.decodeFromString<List<WakeAlarm>>(it) }.getOrNull()
        } ?: emptyList()

    private fun persistAlarms() {
        prefs.edit().putString(KEY_ALARMS, json.encodeToString(alarms)).apply()
    }

    private fun loadRingingState(): WakeAlarmRingingState =
        prefs.getString(KEY_RINGING, null)?.let {
            runCatching { json.decodeFromString<WakeAlarmRingingState>(it) }.getOrNull()
        } ?: WakeAlarmRingingState()

    private fun persistRingingState() {
        prefs.edit().putString(KEY_RINGING, json.encodeToString(ringingState)).apply()
    }

    private companion object {
        const val PREFS_NAME = "aurora_wake_alarms"
        const val KEY_ALARMS = "alarms"
        const val KEY_RINGING = "ringing_state"
        const val KEY_DEFAULT_ALARM_SOUND = "default_alarm_sound_id"
    }
}
