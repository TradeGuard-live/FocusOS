package com.naman.focusos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.TaskAlarmScheduler
import com.naman.focusos.service.FocusTimerService
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FocusDatabase.getInstance(context)
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val futureStr = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val tasks = db.taskDao().getTasksBetweenDatesSync(todayStr, futureStr)

                    val scheduler = TaskAlarmScheduler(context)
                    for (task in tasks) {
                        if (!task.status.isFinished()) {
                            scheduler.scheduleTaskReminders(task)
                        }
                    }

                    // Refresh widget
                    FocusAppWidgetProvider.sendRefreshBroadcast(context)

                    // Recover an active/paused focus session if one exists
                    val activeSession = db.focusSessionDao().getActiveSession()
                    if (activeSession != null) {
                        FocusTimerService.recoverSession(context, activeSession.id)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
