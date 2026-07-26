package com.rusty.aurora.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Notification access isn't a runtime permission with a system dialog -
 * the user has to flip it on manually in Settings. These helpers check
 * the current state and build the Intent that sends them straight to the
 * right screen.
 */
object NotificationAccessUtil {

    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun buildNotificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
