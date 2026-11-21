package com.naman.focusos.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.FocusNotificationManager
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.naman.focusos.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.naman.focusos.ACTION_SNOOZE_TASK"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val notificationManager = FocusNotificationManager(context)

        when (intent.action) {
            ACTION_COMPLETE_TASK -> {
                notificationManager.cancelNotification(taskId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = FocusDatabase.getInstance(context)
                        val task = db.taskDao().getTaskById(taskId)
                        if (task != null) {
                            val completed = task.markCompleted(LocalDateTime.now())
                            db.taskDao().updateTask(completed)
                            com.naman.focusos.notification.TaskAlarmScheduler(context).cancelTaskReminders(taskId)
                            FocusAppWidgetProvider.sendRefreshBroadcast(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE_TASK -> {
                notificationManager.cancelNotification(taskId)
                // Snooze: schedule alarm for 10 minutes later
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeTimeMillis = System.currentTimeMillis() + 10 * 60 * 1000L

                val alarmIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    action = TaskAlarmReceiver.ACTION_TASK_ALARM
                    putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
                    putExtra(TaskAlarmReceiver.EXTRA_TITLE, "Snoozed Focus Task")
                    putExtra(TaskAlarmReceiver.EXTRA_MESSAGE, "10-minute snooze completed. Ready to focus?")
                    putExtra(TaskAlarmReceiver.EXTRA_ALARM_TYPE, "SNOOZE")
                }

                val snoozeRequestCode = ((taskId xor (taskId ushr 32)).toInt() and 0x00FFFFFF) * 100 + 99
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    snoozeRequestCode,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
            }
        }
    }
}
