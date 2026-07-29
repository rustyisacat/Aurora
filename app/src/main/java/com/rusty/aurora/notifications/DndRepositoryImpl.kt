package com.rusty.aurora.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.rusty.aurora.util.DndAccessUtil

class DndRepositoryImpl(private val context: Context) : DndRepository {

    override fun isEnabled(): Boolean {
        val filter = notificationManager()?.currentInterruptionFilter ?: return false
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    override fun setEnabled(enabled: Boolean) {
        if (!DndAccessUtil.isDndAccessGranted(context)) return
        // PRIORITY, not NONE - the standard "Do Not Disturb" behavior still
        // lets alarms and priority interruptions through, rather than
        // going fully silent.
        notificationManager()?.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    private fun notificationManager(): NotificationManager? = context.getSystemService()
}
