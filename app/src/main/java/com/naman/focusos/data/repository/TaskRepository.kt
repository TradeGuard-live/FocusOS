package com.naman.focusos.data.repository

import android.content.Context
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import com.naman.focusos.domain.scheduler.RecurrenceEngine
import com.naman.focusos.notification.TaskAlarmScheduler
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskRepository(private val context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val habitDao = db.habitDao()
    private val recurrenceDao = db.recurrenceRuleDao()
    private val alarmScheduler = TaskAlarmScheduler(context)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksForDate(date)

    fun getTasksForDateSync(date: String): List<TaskEntity> = taskDao.getTasksForDateSync(date)

    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<TaskEntity>> =
        taskDao.getTasksBetweenDates(startDate, endDate)

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getAllRecurrenceRules(): Flow<List<RecurrenceRuleEntity>> = recurrenceDao.getAllRulesFlow()

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    suspend fun getTaskById(id: Long): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        val id = taskDao.insertTask(task)
        val created = task.copy(id = id)
        alarmScheduler.scheduleTaskReminders(created)
        FocusAppWidgetProvider.sendRefreshBroadcast(context)
        id
    }

    suspend fun createRecurringTask(
        baseTask: TaskEntity,
        rule: RecurrenceRuleEntity,
        horizonDays: Long = 60
    ): Long = withContext(Dispatchers.IO) {
        val ruleId = recurrenceDao.insertRule(rule)
        val ruleWithId = rule.copy(id = ruleId)

        val fromDate = LocalDate.parse(baseTask.scheduledDate, dateFormatter)
        val toDate = fromDate.plusDays(horizonDays)

        val occurrences = RecurrenceEngine.generateOccurrences(
            rule = ruleWithId,
            baseTask = baseTask,
            fromDate = fromDate,
            toDate = toDate
        )

        val insertedIds = taskDao.insertTasks(occurrences)

        // Schedule alarms for the first occurrence
        occurrences.firstOrNull()?.let { first ->
            val firstId = insertedIds.firstOrNull() ?: 0L
            alarmScheduler.scheduleTaskReminders(first.copy(id = firstId))
        }

        // Auto-sync recurring series into Active Habits
        val allHabits = habitDao.getAllHabitsSync()
        val titleKey = baseTask.title.trim().lowercase()
        if (allHabits.none { it.title.trim().lowercase() == titleKey }) {
            val newHabit = HabitSyncEngine.createHabitFromTasks(listOf(baseTask), rule)
            habitDao.insertHabit(newHabit)
        }

        FocusAppWidgetProvider.sendRefreshBroadcast(context)
        ruleId
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        updateTaskSeries(task, null)
    }

    /**
     * Updates a task and propagates changes across all days in its recurring series.
     * Updates title, description, category, priority, timing, duration, deadline, and notifications
     * while preserving each day's individual task ID, scheduled date, and completion status.
     */
    suspend fun updateTaskSeries(
        task: TaskEntity,
        newRule: RecurrenceRuleEntity? = null
    ) = withContext(Dispatchers.IO) {
        val allTasksBefore = taskDao.getAllTasksSync()
        val originalTask = allTasksBefore.firstOrNull { it.id == task.id }
        val originalTitleKey = (originalTask?.title ?: task.title).trim().lowercase()

        // 1. Update the target task in database
        taskDao.updateTask(task)

        // 2. Handle recurrence rule updates if provided or existing
        val targetRuleId = task.recurrenceRuleId ?: originalTask?.recurrenceRuleId ?: newRule?.id?.takeIf { it > 0 }
        var effectiveRuleId = targetRuleId

        if (newRule != null) {
            if (targetRuleId != null && targetRuleId > 0) {
                recurrenceDao.updateRule(newRule.copy(id = targetRuleId))
            } else {
                val insertedRuleId = recurrenceDao.insertRule(newRule)
                effectiveRuleId = insertedRuleId
                taskDao.updateTask(task.copy(recurrenceRuleId = insertedRuleId))
            }
        }

        // 3. Find other occurrences in the series to update across all days
        val seriesTasks = allTasksBefore.filter { other ->
            other.id != task.id && (
                (effectiveRuleId != null && other.recurrenceRuleId == effectiveRuleId) ||
                other.title.trim().lowercase() == originalTitleKey
            )
        }

        // 4. Propagate edits across ALL days:
        // Update title, description, category, priority, start time, duration, deadline, notifications
        // Preserve each occurrence's id, scheduledDate, status, and actualCompletionTime
        for (other in seriesTasks) {
            val updatedOther = other.copy(
                title = task.title,
                description = task.description,
                categoryId = task.categoryId,
                priority = task.priority,
                startTime = task.startTime,
                durationMinutes = task.durationMinutes,
                deadlineTime = task.deadlineTime,
                notificationTime1 = task.notificationTime1,
                notificationTime2 = task.notificationTime2,
                notificationTime3 = task.notificationTime3,
                recurrenceRuleId = effectiveRuleId ?: other.recurrenceRuleId,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.updateTask(updatedOther)
            if (!updatedOther.status.isCompleted()) {
                alarmScheduler.scheduleTaskReminders(updatedOther)
            }
        }

        // 5. If weekly recurrence rule weekdays were modified, generate occurrences for newly added weekdays
        if (newRule != null && newRule.type == RecurrenceType.WEEKLY && effectiveRuleId != null) {
            val ruleWithId = newRule.copy(id = effectiveRuleId)
            val fromDate = LocalDate.now()
            val toDate = fromDate.plusDays(60)
            val newOccurrences = RecurrenceEngine.generateOccurrences(
                rule = ruleWithId,
                baseTask = task.copy(recurrenceRuleId = effectiveRuleId),
                fromDate = fromDate,
                toDate = toDate
            )
            val existingDates = (seriesTasks.map { it.scheduledDate } + task.scheduledDate).toSet()
            val toInsert = newOccurrences.filter { it.scheduledDate !in existingDates }
            if (toInsert.isNotEmpty()) {
                taskDao.insertTasks(toInsert)
            }
        }

        // 6. Update alarms for current task
        if (task.status.isCompleted()) {
            alarmScheduler.cancelTaskReminders(task.id)
        } else {
            alarmScheduler.scheduleTaskReminders(task)
        }

        FocusAppWidgetProvider.sendRefreshBroadcast(context)

        // 7. Sync with Active Habits & notify widget
        val habitRepo = HabitRepository(context)
        habitRepo.syncTasksToHabits(taskDao.getAllTasksSync())
    }

    suspend fun toggleTaskCompletion(taskId: Long): TaskEntity? = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext null
        val updated = if (task.status.isCompleted()) {
            task.markIncomplete()
        } else {
            task.markCompleted(LocalDateTime.now())
        }
        taskDao.updateTask(updated)
        if (updated.status.isCompleted()) {
            alarmScheduler.cancelTaskReminders(taskId)
        } else {
            alarmScheduler.scheduleTaskReminders(updated)
        }
        FocusAppWidgetProvider.sendRefreshBroadcast(context)

        // Automatically sync completion to Active Habits
        syncTaskCompletionToHabit(updated)

        updated
    }

    private suspend fun syncTaskCompletionToHabit(task: TaskEntity) {
        val titleKey = task.title.trim().lowercase()
        val allMatching = taskDao.getAllTasksSync().filter { it.title.trim().lowercase() == titleKey }

        // Only sync if recurring or multi-day (one-day tasks must NOT be shown as habits)
        if (HabitSyncEngine.isHabitQualifying(allMatching)) {
            val allHabits = habitDao.getAllHabitsSync()
            var habit = allHabits.firstOrNull { it.title.trim().lowercase() == titleKey }
            val ruleId = allMatching.mapNotNull { it.recurrenceRuleId }.firstOrNull() ?: task.recurrenceRuleId
            val rule = ruleId?.let { recurrenceDao.getRuleByIdSync(it) }

            if (habit == null) {
                val newHabit = HabitSyncEngine.createHabitFromTasks(allMatching.ifEmpty { listOf(task) }, rule)
                val newId = habitDao.insertHabit(newHabit)
                habit = newHabit.copy(id = newId)
            } else if (!habit.isActive) {
                val reactivated = habit.copy(isActive = true)
                habitDao.updateHabit(reactivated)
                habit = reactivated
            }

            if (task.status.isCompleted()) {
                habitDao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habit.id,
                        date = task.scheduledDate,
                        isCompleted = true,
                        actualValue = 1.0f,
                        completedAt = task.actualCompletionTime ?: System.currentTimeMillis()
                    )
                )
            } else {
                habitDao.deleteCompletion(habit.id, task.scheduledDate)
            }
        }
    }

    private fun pickCategoryColor(categoryId: Long): String {
        return when (categoryId) {
            1L -> "#38BDF8"
            2L -> "#8B5CF6"
            3L -> "#10B981"
            4L -> "#F59E0B"
            5L -> "#06B6D4"
            6L -> "#EC4899"
            else -> "#38BDF8"
        }
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
        alarmScheduler.cancelTaskReminders(task.id)
        FocusAppWidgetProvider.sendRefreshBroadcast(context)
    }

    suspend fun deleteRecurringSeries(ruleId: Long, preserveHistory: Boolean = true) = withContext(Dispatchers.IO) {
        if (preserveHistory) {
            // Delete only non-completed future tasks for this rule
            val todayStr = LocalDate.now().format(dateFormatter)
            val allForRule = taskDao.getAllTasksSync().filter { it.recurrenceRuleId == ruleId }
            for (t in allForRule) {
                if (!t.status.isCompleted() && t.scheduledDate >= todayStr) {
                    taskDao.deleteTask(t)
                    alarmScheduler.cancelTaskReminders(t.id)
                }
            }
        } else {
            taskDao.deleteTasksByRecurrenceRule(ruleId)
        }
        recurrenceDao.deleteRuleById(ruleId)
        FocusAppWidgetProvider.sendRefreshBroadcast(context)
    }
}
