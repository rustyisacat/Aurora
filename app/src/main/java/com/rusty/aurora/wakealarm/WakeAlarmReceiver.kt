package com.rusty.aurora.wakealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rusty.aurora.AuroraApplication
import com.rusty.aurora.service.AuroraBackgroundService

/**
 * Fired by AlarmManager (via WakeAlarmScheduler's setAlarmClock()) at the
 * scheduled time. Just flips the repository's ringing state - the actual
 * noise happens on the Echo Show, which picks the change up on its next
 * /dashboard poll (see WakeAlarmRepository's doc comment).
 */
class WakeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(WakeAlarmScheduler.EXTRA_ALARM_ID) ?: return
        val container = (context.applicationContext as AuroraApplication).container
        container.wakeAlarmRepository.handleFired(alarmId)

        // Defensive: makes sure the HTTP server is actually up to serve the
        // ringing state, in case the background service was killed for
        // memory overnight. AlarmManager-fired broadcasts are one of the
        // exemptions Android grants for starting a foreground service from
        // the background, same as this app's own alarm firing counts as
        // "the user is expecting this right now."
        AuroraBackgroundService.start(context)
    }
}
