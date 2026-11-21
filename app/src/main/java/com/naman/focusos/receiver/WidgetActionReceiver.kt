package com.naman.focusos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.TaskAlarmScheduler
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Secure unexported BroadcastReceiver for handling interactive widget actions.
 * Marked exported=false in AndroidManifest to prevent unauthorized external manipulation.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_COMPLETE_TASK = "com.naman.focusos.ACTION_WIDGET_COMPLETE_TASK"
        const val ACTION_WIDGET_REFRESH = "com.naman.focusos.ACTION_WIDGET_REFRESH"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_WIDGET_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                // Strict validation: Reject invalid or non-positive IDs
                if (taskId <= 0L) {
                    Log.w("WidgetActionReceiver", "Rejected widget complete task with invalid taskId: $taskId")
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = FocusDatabase.getInstance(context)
                        val task = db.taskDao().getTaskById(taskId)
                        if (task != null) {
                            val updated = if (task.status.isCompleted()) {
                                task.markIncomplete()
                            } else {
                                val completed = task.markCompleted(LocalDateTime.now())
                                TaskAlarmScheduler(context).cancelTaskReminders(taskId)
                                completed
                            }
                            db.taskDao().updateTask(updated)
                        }
                        // Refresh all widgets
                        FocusAppWidgetProvider.sendRefreshBroadcast(context)
                    } catch (e: Exception) {
                        Log.e("WidgetActionReceiver", "Error completing task from widget", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_WIDGET_REFRESH -> {
                FocusAppWidgetProvider.sendRefreshBroadcast(context)
            }
        }
    }
}

