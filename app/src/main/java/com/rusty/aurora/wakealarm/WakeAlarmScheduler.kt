package com.rusty.aurora.wakealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rusty.aurora.ui.MainActivity

/**
 * Thin wrapper around AlarmManager.setAlarmClock() - the API Android's own
 * Clock app uses, which (unlike setExact/setExactAndAllowWhileIdle) needs
 * no special permission and is explicitly exempt from Doze/battery
 * restrictions, because it's meant exactly for this: a genuine alarm clock
 * the user is relying on to wake up.
 */
class WakeAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: WakeAlarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerAt = NextTriggerCalculator.nextTriggerMillis(
            alarm.hour, alarm.minute, alarm.daysOfWeek, System.currentTimeMillis()
        )
        arm(requestCode(alarm.id), alarm.id, triggerAt)
    }

    fun scheduleSnooze(alarm: WakeAlarm, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        arm(snoozeRequestCode(alarm.id), alarm.id, triggerAt)
    }

    fun cancel(alarmId: String) {
        alarmManager.cancel(firePendingIntent(requestCode(alarmId), alarmId))
    }

    private fun arm(requestCode: Int, alarmId: String, triggerAtMillis: Long) {
        val showIntent = PendingIntent.getActivity(
            context, requestCode, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            firePendingIntent(requestCode, alarmId)
        )
    }

    private fun firePendingIntent(requestCode: Int, alarmId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, WakeAlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // Snooze gets its own request code so it doesn't clobber (or get
    // clobbered by) the alarm's own regular repeat schedule while pending.
    private fun requestCode(alarmId: String): Int = alarmId.hashCode()
    private fun snoozeRequestCode(alarmId: String): Int = (alarmId + SNOOZE_SUFFIX).hashCode()

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val SNOOZE_SUFFIX = ":snooze"
    }
}
