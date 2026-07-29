package com.rusty.aurora.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.getSystemService

/** Do Not Disturb access, same shape as NotificationAccessUtil - not a
 *  runtime dialog, the user has to flip it on manually in Settings. */
object DndAccessUtil {

    fun isDndAccessGranted(context: Context): Boolean =
        context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted ?: false

    fun buildDndAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}
