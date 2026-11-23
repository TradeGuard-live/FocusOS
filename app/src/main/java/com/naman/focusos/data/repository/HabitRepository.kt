package com.naman.focusos.data.repository

import android.content.Context
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.excel.HabitImportPreview
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HabitRepository(private val context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val habitDao = db.habitDao()
    private val recurrenceDao = db.recurrenceRuleDao()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getActiveHabits(): Flow<List<HabitEntity>> = habitDao.getActiveHabits()

    fun getCompletionsForMonth(monthPrefix: String): Flow<List<HabitCompletionEntity>> =
        habitDao.getCompletionsForMonth(monthPrefix)

    fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>> =
        habitDao.getCompletionsForDate(date)

    suspend fun insertHabit(habit: HabitEntity): Long = withContext(Dispatchers.IO) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habit)
    }

    suspend fun getHabitById(id: Long): HabitEntity? = withContext(Dispatchers.IO) {
        habitDao.getHabitById(id)
    }

    /**
     * Synchronizes recurring & multi-day tasks into active habits.
     * Rules:
     * 1. Multi-day / recurring tasks (recurrenceRuleId != null or distinctDates > 1) are automatically in Active Habits.
     * 2. When a task is completed, its completion is automatically recorded in habit_completions.
     * 3. Single-day tasks (recurrenceRuleId == null and only scheduled on 1 date) are strictly excluded.
     */
    suspend fun syncTasksToHabits(tasks: List<TaskEntity>) = withContext(Dispatchers.IO) {
        val tasksByTitle = tasks.groupBy { it.title.trim().lowercase() }
        val existingHabits = habitDao.getAllHabitsSync()
        val habitsByTitle = existingHabits.associateBy { it.title.trim().lowercase() }.toMutableMap()

        // 1. Clean up auto-synced routine habits whose tasks were deleted or no longer qualify as a habit
        for (existingHabit in existingHabits) {
            val titleKey = existingHabit.title.trim().lowercase()
            val taskList = tasksByTitle[titleKey] ?: emptyList()
            val isHabit = HabitSyncEngine.isHabitQualifying(taskList)

            if (!isHabit) {
                // If auto-synced from tasks/routines, remove cleanly
                if (existingHabit.description.startsWith("Scheduled:") ||
                    existingHabit.description == "Auto-synced routine" ||
                    existingHabit.description.startsWith("Monthly") ||
                    existingHabit.description.startsWith("Yearly") ||
                    existingHabit.description == "Daily routine" ||
                    existingHabit.description == "Custom routine"
                ) {
                    habitDao.deleteHabit(existingHabit)
                    habitsByTitle.remove(titleKey)
                }
            }
        }

        // 2. Sync remaining qualifying habits
        for ((titleKey, taskList) in tasksByTitle) {
            val isHabit = HabitSyncEngine.isHabitQualifying(taskList)
            if (!isHabit) continue

            val ruleId = taskList.mapNotNull { it.recurrenceRuleId }.firstOrNull()
            val rule = ruleId?.let { recurrenceDao.getRuleByIdSync(it) }
            val targetHabit = HabitSyncEngine.createHabitFromTasks(taskList, rule)

            var habit = habitsByTitle[titleKey]
            if (habit == null) {
                val newId = habitDao.insertHabit(targetHabit)
                habit = targetHabit.copy(id = newId)
                habitsByTitle[titleKey] = habit
            } else {
                val updated = targetHabit.copy(id = habit.id, isActive = true)
                if (habit.daysOfWeekMask != updated.daysOfWeekMask ||
                    habit.frequency != updated.frequency ||
                    habit.description != updated.description ||
                    !habit.isActive
                ) {
                    habitDao.updateHabit(updated)
                    habitsByTitle[titleKey] = updated
                    habit = updated
                }
            }

            // Sync completions for this habit
            val existingCompletions = habitDao.getCompletionsForHabitSync(habit.id).associateBy { it.date }
            val validDates = taskList.map { it.scheduledDate }.toSet()

            // Remove completions for dates that no longer have tasks
            for (comp in existingCompletions.values) {
                if (comp.date !in validDates) {
                    habitDao.deleteCompletion(habit.id, comp.date)
                }
            }

            for (task in taskList) {
                val isCompleted = task.status.isCompleted()
                val comp = existingCompletions[task.scheduledDate]

                if (isCompleted && (comp == null || !comp.isCompleted)) {
                    habitDao.insertCompletion(
                        HabitCompletionEntity(
                            habitId = habit.id,
                            date = task.scheduledDate,
                            isCompleted = true,
                            actualValue = 1.0f,
                            completedAt = task.actualCompletionTime ?: System.currentTimeMillis()
                        )
                    )
                } else if (!isCompleted && comp != null && comp.isCompleted) {
                    habitDao.deleteCompletion(habit.id, task.scheduledDate)
                }
            }
        }
    }

    fun pickCategoryColor(categoryId: Long): String {
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

    suspend fun toggleHabitCompletion(habitId: Long, date: String): Boolean = withContext(Dispatchers.IO) {
        val habit = habitDao.getHabitById(habitId)
        if (habit != null && habit.frequency == HabitFrequency.SPECIFIC_DAYS) {
            val localDate = try { LocalDate.parse(date, dateFormatter) } catch (e: Exception) { null }
            if (localDate != null && !habit.isScheduledForDay(localDate.dayOfWeek.value)) {
                return@withContext false
            }
        }
        val existing = habitDao.getCompletionsForDateSync(date).firstOrNull { it.habitId == habitId }
        if (existing != null && existing.isCompleted) {
            habitDao.deleteCompletion(habitId, date)
            false
        } else {
            val completion = HabitCompletionEntity(
                habitId = habitId,
                date = date,
                isCompleted = true,
                actualValue = 1.0f,
                completedAt = System.currentTimeMillis()
            )
            habitDao.insertCompletion(completion)
            true
        }
    }

    suspend fun importHabitsFromPreview(
        preview: HabitImportPreview,
        targetYearMonth: YearMonth = YearMonth.now()
    ): Int = withContext(Dispatchers.IO) {
        val existingHabits = habitDao.getAllHabitsSync()
        val habitMap = existingHabits.associateBy { it.title.trim().lowercase() }.toMutableMap()
        var importedCount = 0

        for (entry in preview.parsedEntries) {
            val titleKey = entry.habitTitle.trim().lowercase()
            val habit = habitMap.getOrPut(titleKey) {
                val newHabit = HabitEntity(
                    title = entry.habitTitle.trim(),
                    description = "Imported from Excel Habit Tracker",
                    colorHex = pickColorForIndex(habitMap.size)
                )
                val newId = habitDao.insertHabit(newHabit)
                newHabit.copy(id = newId)
            }

            // Insert completion records for the specified days
            val completions = mutableListOf<HabitCompletionEntity>()
            for (dayNum in entry.completedDays) {
                if (dayNum in 1..targetYearMonth.lengthOfMonth()) {
                    val date = targetYearMonth.atDay(dayNum).format(dateFormatter)
                    completions.add(
                        HabitCompletionEntity(
                            habitId = habit.id,
                            date = date,
                            isCompleted = true,
                            actualValue = 1.0f
                        )
                    )
                    importedCount++
                }
            }
            if (completions.isNotEmpty()) {
                habitDao.insertCompletions(completions)
            }
        }
        importedCount
    }

    private fun pickColorForIndex(index: Int): String {
        val colors = listOf(
            "#38BDF8", "#10B981", "#8B5CF6", "#F59E0B",
            "#06B6D4", "#EC4899", "#3B82F6", "#14B8A6"
        )
        return colors[index % colors.size]
    }
}
