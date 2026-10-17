package com.besafe.app.safety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.besafe.app.MainActivity
import com.besafe.app.R

object Notifications {

    const val SESSION_ID = 1
    const val RESULT_ID = 2

    private const val CHANNEL_SESSION = "session"
    private const val CHANNEL_URGENT = "urgent"
    private const val CHANNEL_RESULT = "result"

    private val urgentVibration = longArrayOf(0, 600, 300, 600, 300, 600)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_SESSION, "Active check-in", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Countdown while a check-in timer is running"
                },
                NotificationChannel(CHANNEL_URGENT, "Are you safe?", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Warning before an alert is sent to your contacts"
                    enableVibration(true)
                    vibrationPattern = urgentVibration
                },
                NotificationChannel(CHANNEL_RESULT, "Alert status", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Whether an alert was sent"
                },
            ),
        )
    }

    fun checkIn(context: Context, endsAtMillis: Long): Notification =
        base(context, CHANNEL_SESSION)
            .setContentTitle("Check-in running")
            .setContentText("Tap \"I'm safe\" when you arrive, or your contacts will be alerted.")
            .setOngoing(true)
            .setSilent(true)
            .setWhen(endsAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(0, "I'm safe", serviceIntent(context, CheckInService.ACTION_SAFE))
            .addAction(0, "+10 min", serviceIntent(context, CheckInService.ACTION_EXTEND))
            .build()

    fun grace(context: Context, alertAtMillis: Long): Notification =
        base(context, CHANNEL_URGENT)
            .setContentTitle("Are you safe?")
            .setContentText("Your contacts will be alerted unless you respond.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(urgentVibration)
            .setWhen(alertAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(0, "I'm safe", serviceIntent(context, CheckInService.ACTION_SAFE))
            .addAction(0, "+10 min", serviceIntent(context, CheckInService.ACTION_EXTEND))
            .build()

    fun sending(context: Context): Notification =
        base(context, CHANNEL_URGENT)
            .setContentTitle("Sending alert…")
            .setContentText("Getting your location and texting your contacts.")
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun result(context: Context, state: SessionState): Notification? {
        val (title, text) = when (state) {
            is SessionState.AlertSent -> "Alert sent" to "Texted ${state.recipients} contact(s)." +
                if (state.hadLocation) "" else " Location was unavailable."
            is SessionState.Failed -> "Alert NOT sent" to state.message
            else -> return null
        }
        return base(context, CHANNEL_RESULT)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
    }

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(openApp(context))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun serviceIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, CheckInService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
