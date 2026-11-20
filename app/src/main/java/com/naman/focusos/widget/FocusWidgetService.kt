package com.naman.focusos.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.naman.focusos.MainActivity
import com.naman.focusos.R
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.TaskEntity
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FocusWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FocusRemoteViewsFactory(applicationContext)
    }
}

class FocusRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    @Volatile
    private var tasks: List<TaskEntity> = emptyList()

    override fun onCreate() {
        // In RemoteViewsFactory, onCreate() runs synchronously on the Main Thread during onBind().
        // Database queries or disk I/O must not be performed here.
        // onDataSetChanged() is automatically invoked by the system on a background Binder thread.
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val db = FocusDatabase.getInstance(context)
            tasks = runBlocking(Dispatchers.IO) {
                db.taskDao().getTasksForDateSync(todayStr)
            }
        } catch (e: Exception) {
            Log.e("FocusWidgetService", "Error loading tasks for widget", e)
            tasks = emptyList()
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val currentTasks = tasks
        if (position !in currentTasks.indices) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }

        val task = currentTasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        // Set title
        views.setTextViewText(R.id.widget_task_title, task.title)

        // Set time
        val timeDisplay = when {
            task.startTime != null && task.deadlineTime != null -> "${task.startTime} - ${task.deadlineTime}"
            task.startTime != null -> task.startTime
            task.deadlineTime != null -> "Due ${task.deadlineTime}"
            else -> "Anytime (${task.durationMinutes}m)"
        }
        views.setTextViewText(R.id.widget_task_time, timeDisplay)

        // Set priority badge
        views.setTextViewText(R.id.widget_task_priority, task.priority.name)
        val priorityColor = when (task.priority) {
            Priority.CRITICAL -> 0xFFEF4444.toInt() // Coral
            Priority.HIGH -> 0xFFF59E0B.toInt()     // Amber
            Priority.MEDIUM -> 0xFF38BDF8.toInt()   // Electric Blue
            Priority.LOW -> 0xFF94A3B8.toInt()      // Slate
        }
        views.setTextColor(R.id.widget_task_priority, priorityColor)

        // Set completed icon & text strike
        if (task.status.isCompleted()) {
            views.setImageViewResource(R.id.widget_task_checkbox, R.drawable.ic_check_circle)
            views.setTextColor(R.id.widget_task_title, 0xFF64748B.toInt())
        } else {
            views.setImageViewResource(R.id.widget_task_checkbox, R.drawable.ic_circle_outline)
            views.setTextColor(R.id.widget_task_title, 0xFFF1F5F9.toInt())
        }

        // Fill-in Intent for Checkbox Click (Directly toggle completion in DB!)
        val completeFillIn = Intent().apply {
            action = FocusAppWidgetProvider.ACTION_WIDGET_COMPLETE_TASK
            putExtra(FocusAppWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_task_checkbox, completeFillIn)

        // Fill-in Intent for Row Click (Opens app)
        val rowFillIn = Intent(context, MainActivity::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, rowFillIn)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val currentTasks = tasks
        return if (position in currentTasks.indices) currentTasks[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
