package com.naman.focusos.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.naman.focusos.MainActivity
import com.naman.focusos.R
import com.naman.focusos.receiver.NotificationActionReceiver

class FocusNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_REMINDERS_ID = "focus_os_reminders_channel"
        const val CHANNEL_DEADLINES_ID = "focus_os_deadlines_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Focus Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled focus tasks"
                enableVibration(true)
                setSound(soundUri, null)
            }

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES_ID,
                "Focus Deadlines & Overdue",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when task deadlines arrive"
                enableVibration(true)
                setSound(soundUri, null)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(deadlineChannel)
        }
    }

    fun showTaskNotification(
        taskId: Long,
        title: String,
        message: String,
        isDeadline: Boolean
    ) {
        val channelId = if (isDeadline) CHANNEL_DEADLINES_ID else CHANNEL_REMINDERS_ID

        val safeNotificationId = ((taskId xor (taskId ushr 32)).toInt() and 0x00FFFFFF)
        val completeRequestCode = safeNotificationId * 100 + 1
        val snoozeRequestCode = safeNotificationId * 100 + 2

        // Tap content intent -> opens MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            safeNotificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Complete
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            completeRequestCode,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10m
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Public version for lock screen privacy (sanitizes private titles & notes)
        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Focus OS")
            .setContentText("Focus task notification")
            .setColor(0xFF38BDF8.toInt())
            .build()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setColor(0xFF38BDF8.toInt())
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_check_circle, "Complete", completePendingIntent)
            .addAction(R.drawable.ic_circle_outline, "Snooze 10m", snoozePendingIntent)

        notificationManager.notify(safeNotificationId, builder.build())
    }

    fun cancelNotification(taskId: Long) {
        val safeNotificationId = ((taskId xor (taskId ushr 32)).toInt() and 0x00FFFFFF)
        notificationManager.cancel(safeNotificationId)
    }
}
