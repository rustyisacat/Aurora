package com.rusty.aurora.wakealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rusty.aurora.AuroraApplication

/** AlarmManager alarms don't survive a reboot on their own - this re-arms
 *  every enabled wake alarm so a phone restart never silently loses one. */
class WakeAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        (context.applicationContext as AuroraApplication).container.wakeAlarmRepository.rearmAll()
    }
}
