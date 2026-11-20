package com.naman.focusos.domain.scheduler

import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object HabitSyncEngine {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Determines whether a set of tasks for a given title qualifies as an Active Habit / Routine.
     * Rules:
     * - Recurring tasks (recurrenceRuleId != null) qualify as active habits.
     * - Tasks spanning multiple distinct dates (distinctDates > 1) qualify as active habits.
     * - Tasks of one day (recurrenceRuleId == null and only on 1 date) are strictly excluded.
     */
    fun isHabitQualifying(tasksForTitle: List<TaskEntity>): Boolean {
        if (tasksForTitle.isEmpty()) return false
        val hasRecurrence = tasksForTitle.any { it.recurrenceRuleId != null }
        val distinctDates = tasksForTitle.map { it.scheduledDate }.distinct().size
        return hasRecurrence || distinctDates > 1
    }

    /**
     * Filters a list of all tasks into only those groups that qualify for the Active Habits section.
     * Keys are lowercase trimmed titles.
     */
    fun filterActiveHabitTasks(allTasks: List<TaskEntity>): Map<String, List<TaskEntity>> {
        return allTasks
            .groupBy { it.title.trim().lowercase() }
            .filter { (_, taskList) -> isHabitQualifying(taskList) }
    }

    /**
     * Creates a human-readable description for a weekly bitmask (e.g., "Scheduled: Mon", "Scheduled: Mon, Wed, Fri").
     */
    fun formatWeeklySchedule(mask: Int): String {
        if (mask == 127) return "Daily routine"
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val selected = mutableListOf<String>()
        for (i in 1..7) {
            if ((mask and (1 shl (i - 1))) != 0) {
                selected.add(dayNames[i - 1])
            }
        }
        return if (selected.isEmpty()) "Custom routine" else "Scheduled: ${selected.joinToString(", ")}"
    }

    /**
     * Creates a HabitEntity representation from a qualifying task series.
     */
    fun createHabitFromTasks(tasks: List<TaskEntity>, rule: RecurrenceRuleEntity? = null): HabitEntity {
        require(tasks.isNotEmpty()) { "Cannot create habit from empty task list" }
        val sample = tasks.first()
        val hasRecurrence = tasks.any { it.recurrenceRuleId != null }

        val mask = when {
            rule != null && rule.type == RecurrenceType.WEEKLY -> rule.daysOfWeekMask
            rule != null && rule.type == RecurrenceType.DAILY -> 127
            rule != null -> 127
            hasRecurrence -> 127
            else -> {
                val days = tasks.mapNotNull {
                    try {
                        LocalDate.parse(it.scheduledDate, dateFormatter).dayOfWeek.value
                    } catch (e: Exception) {
                        null
                    }
                }.distinct()
                if (days.isNotEmpty() && days.size < 7) {
                    RecurrenceRuleEntity.createWeeklyMask(days)
                } else {
                    127
                }
            }
        }

        val frequency = when {
            rule?.type == RecurrenceType.DAILY -> HabitFrequency.DAILY
            rule?.type == RecurrenceType.WEEKLY -> HabitFrequency.SPECIFIC_DAYS
            rule?.type == RecurrenceType.MONTHLY_DATE || rule?.type == RecurrenceType.MONTHLY_DAY_OF_WEEK || rule?.type == RecurrenceType.YEARLY -> HabitFrequency.CUSTOM
            hasRecurrence -> HabitFrequency.DAILY
            mask != 127 -> HabitFrequency.SPECIFIC_DAYS
            else -> HabitFrequency.DAILY
        }

        val scheduleDesc = when {
            rule?.type == RecurrenceType.WEEKLY -> formatWeeklySchedule(mask)
            rule?.type == RecurrenceType.MONTHLY_DATE -> "Monthly on day ${rule.dayOfMonth ?: 1}"
            rule?.type == RecurrenceType.MONTHLY_DAY_OF_WEEK -> "Monthly nth weekday"
            rule?.type == RecurrenceType.YEARLY -> "Yearly routine"
            hasRecurrence -> "Daily routine"
            mask != 127 -> formatWeeklySchedule(mask)
            else -> "Daily routine"
        }

        return HabitEntity(
            title = sample.title.trim(),
            description = scheduleDesc,
            categoryId = sample.categoryId,
            colorHex = pickCategoryColor(sample.categoryId),
            frequency = frequency,
            daysOfWeekMask = mask,
            isActive = true
        )
    }

    /**
     * Determines whether a habit is scheduled for a specific date across all recurrence timeframes:
     * - Weekly (e.g. Monday only, or Mon & Wed): only scheduled on matching weekdays.
     * - Monthly by date: only scheduled on the target day of the month.
     * - Monthly by day of week: only scheduled on the nth weekday of the month.
     * - Yearly: only scheduled on the target month and day.
     * - Daily: scheduled every day.
     * - Custom / Specific dates: only scheduled on dates where tasks exist.
     *
     * Used by RoutinesScreen and HabitGrid to disable completion options on non-scheduled days.
     */
    fun isHabitScheduledOnDate(
        habit: HabitEntity,
        date: LocalDate,
        rule: RecurrenceRuleEntity? = null,
        scheduledDates: Set<String>? = null
    ): Boolean {
        val dateStr = date.format(dateFormatter)

        // 1. If a recurrence rule is present, evaluate according to rule type
        if (rule != null) {
            val ruleStart = try {
                LocalDate.parse(rule.startDate, dateFormatter)
            } catch (e: Exception) {
                LocalDate.MIN
            }
            val ruleEnd = rule.endDate?.let {
                try { LocalDate.parse(it, dateFormatter) } catch (e: Exception) { null }
            }

            if (date.isBefore(ruleStart)) return false
            if (ruleEnd != null && date.isAfter(ruleEnd)) return false
            if (rule.hasException(dateStr)) return false

            return when (rule.type) {
                RecurrenceType.DAILY -> true
                RecurrenceType.WEEKLY -> rule.isDayOfWeekSelected(date.dayOfWeek.value)
                RecurrenceType.MONTHLY_DATE -> {
                    val targetDay = rule.dayOfMonth ?: ruleStart.dayOfMonth
                    val safeDay = targetDay.coerceAtMost(date.lengthOfMonth())
                    date.dayOfMonth == safeDay
                }
                RecurrenceType.MONTHLY_DAY_OF_WEEK -> {
                    val targetWeekday = DayOfWeek.of(rule.weekdayForNth?.coerceIn(1, 7) ?: 1)
                    val nth = rule.nthWeekday ?: 1
                    val expectedDate = if (nth == -1) {
                        date.with(TemporalAdjusters.lastInMonth(targetWeekday))
                    } else {
                        date.with(TemporalAdjusters.dayOfWeekInMonth(nth, targetWeekday))
                    }
                    date == expectedDate
                }
                RecurrenceType.YEARLY -> {
                    val targetMonth = rule.monthOfYear ?: ruleStart.monthValue
                    val targetDay = (rule.dayOfMonth ?: ruleStart.dayOfMonth).coerceAtMost(date.lengthOfMonth())
                    date.monthValue == targetMonth && date.dayOfMonth == targetDay
                }
                RecurrenceType.NONE -> scheduledDates?.contains(dateStr) == true
            }
        }

        // 2. If explicit task scheduled dates are present (non-recurring multi-day tasks)
        if (!scheduledDates.isNullOrEmpty()) {
            return scheduledDates.contains(dateStr)
        }

        // 3. Fallback to HabitEntity's own properties
        return when (habit.frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.SPECIFIC_DAYS -> habit.isScheduledForDay(date.dayOfWeek.value)
            HabitFrequency.CUSTOM -> {
                if (habit.daysOfWeekMask != 127 && habit.daysOfWeekMask != 0) {
                    habit.isScheduledForDay(date.dayOfWeek.value)
                } else {
                    true
                }
            }
        }
    }

    /**
     * Generates expected HabitCompletionEntities from completed tasks for a habit.
     */
    fun generateCompletionsForHabit(habitId: Long, tasks: List<TaskEntity>): List<HabitCompletionEntity> {
        return tasks.filter { it.status.isCompleted() }.map { task ->
            HabitCompletionEntity(
                habitId = habitId,
                date = task.scheduledDate,
                isCompleted = true,
                actualValue = 1.0f,
                completedAt = task.actualCompletionTime ?: System.currentTimeMillis()
            )
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
}
