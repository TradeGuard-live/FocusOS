package com.naman.focusos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.naman.focusos.MainActivity
import com.naman.focusos.R
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class FocusAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_COMPLETE_TASK = "com.naman.focusos.ACTION_WIDGET_COMPLETE_TASK"
        const val ACTION_WIDGET_REFRESH = "com.naman.focusos.ACTION_WIDGET_REFRESH"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

        fun sendRefreshBroadcast(context: Context) {
            val intent = Intent(context, FocusAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                Log.e("FocusAppWidgetProvider", "Error during onUpdate", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = FocusDatabase.getInstance(context)
                            val task = db.taskDao().getTaskById(taskId)
                            if (task != null) {
                                val updated = if (task.status.isCompleted()) {
                                    task.markIncomplete()
                                } else {
                                    task.markCompleted(LocalDateTime.now())
                                }
                                db.taskDao().updateTask(updated)
                            }
                            // Refresh all widgets
                            refreshAllWidgets(context)
                        } catch (e: Exception) {
                            Log.e("FocusAppWidgetProvider", "Error completing task from widget", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_WIDGET_REFRESH -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        refreshAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e("FocusAppWidgetProvider", "Error handling widget refresh", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun refreshAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FocusAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_task_list)
            }
        } catch (e: Exception) {
            Log.e("FocusAppWidgetProvider", "Error refreshing all widgets", e)
        }
    }

    private suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_focus_dashboard)

            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val monthName = today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val formattedDate = "$dayName, $monthName ${today.dayOfMonth}"

            views.setTextViewText(R.id.widget_date, formattedDate)

            // Query today's tasks
            val db = FocusDatabase.getInstance(context)
            val tasks = db.taskDao().getTasksForDateSync(todayStr)
            val dayStats = ProductivityIntelligenceEngine.calculateDayStats(today, tasks, LocalDateTime.now())

            val total = dayStats.totalTasks
            val completed = dayStats.completedOnTime + dayStats.completedLate
            val percentage = dayStats.completionRate.toInt()

            views.setTextViewText(R.id.widget_progress_text, "$completed/$total ($percentage%)")
            views.setProgressBar(R.id.widget_progress_bar, 100, percentage, false)

            if (tasks.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_task_list, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_empty_text, View.GONE)
                views.setViewVisibility(R.id.widget_task_list, View.VISIBLE)
            }

            // Tap on header -> open app
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_date, openAppPendingIntent)

            // Set adapter for task list
            val serviceIntent = Intent(context, FocusWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_text)

            // PendingIntent template for item actions (e.g. checkbox clicks)
            val actionIntent = Intent(context, FocusAppWidgetProvider::class.java)
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, actionPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            Log.e("FocusAppWidgetProvider", "Error updating widget $appWidgetId", e)
        }
    }
}
