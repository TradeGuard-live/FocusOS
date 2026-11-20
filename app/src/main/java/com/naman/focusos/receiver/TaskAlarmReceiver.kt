package com.naman.focusos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.FocusNotificationManager
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_ALARM = "com.naman.focusos.ACTION_TASK_ALARM"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ALARM_TYPE = "EXTRA_ALARM_TYPE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)
                // If task doesn't exist or is already completed/finished, suppress notification
                if (task == null || task.status.isCompleted()) {
                    return@launch
                }

                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Focus OS Task"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Time for your focus task"
                val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: "START"
                val isDeadline = alarmType == "DEADLINE"

                val notificationManager = FocusNotificationManager(context)
                notificationManager.showTaskNotification(
                    taskId = taskId,
                    title = title,
                    message = message,
                    isDeadline = isDeadline
                )

                // Update home-screen widget
                FocusAppWidgetProvider.sendRefreshBroadcast(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
