package com.rusty.aurora.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rusty.aurora.AuroraApplication
import com.rusty.aurora.R
import com.rusty.aurora.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps Aurora's HTTP server running for exactly as long as the phone is
 * on the home Wi-Fi network - the same one the Echo Show is on - and stops
 * it (while staying alive itself, with a quiet notification) the moment
 * it isn't, so Aurora doesn't hold a listening socket open and poll
 * battery/notification/calendar/weather state all day while the phone is
 * out and about, nowhere near the dashboard it's serving.
 *
 * Started once from MainActivity - a foreground Activity is always exempt
 * from Android 12+'s restrictions on starting foreground services from the
 * background, whereas starting this from AuroraApplication.onCreate()
 * could be blocked if the process happens to be woken up for some other
 * reason (e.g. the notification listener binding) while the app has no
 * visible UI. Returns START_STICKY so Android resurrects it after being
 * killed for memory even with the Activity long gone - that's what makes
 * closing the app safe: the server's actual lifecycle already lived in
 * AuroraServerController, independent of any Activity, from the start.
 */
class AuroraBackgroundService : Service() {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Must happen within moments of the service starting, before any
        // suspending/async work - otherwise Android throws
        // ForegroundServiceDidNotStartInTimeException.
        startForeground(NOTIFICATION_ID, buildNotification(isOnHomeNetwork = false))

        val container = (application as AuroraApplication).container
        container.homeNetworkMonitor.start()

        monitorJob = scope.launch {
            container.homeNetworkMonitor.isOnHomeNetwork.collectLatest { onHomeNetwork ->
                if (onHomeNetwork) {
                    container.serverController.start()
                } else {
                    container.serverController.stop()
                }
                updateNotification(onHomeNetwork)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        val container = (application as AuroraApplication).container
        container.homeNetworkMonitor.stop()
        container.serverController.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Aurora background service", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps the dashboard's data server running while you're on your home Wi-Fi"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(isOnHomeNetwork: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (isOnHomeNetwork) {
            "Serving the dashboard on your home Wi-Fi"
        } else {
            "Idle - waiting for home Wi-Fi"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aurora")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(isOnHomeNetwork: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(isOnHomeNetwork))
    }

    companion object {
        private const val CHANNEL_ID = "aurora_background_service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AuroraBackgroundService::class.java))
        }
    }
}
