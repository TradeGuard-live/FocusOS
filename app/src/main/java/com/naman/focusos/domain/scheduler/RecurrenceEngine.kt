package com.naman.focusos.domain.scheduler

import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object RecurrenceEngine {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Generate individual task occurrences for a recurrence rule within a target date range.
     * Preserves independence: each generated occurrence is a distinct task instance.
     */
    fun generateOccurrences(
        rule: RecurrenceRuleEntity,
        baseTask: TaskEntity,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<TaskEntity> {
        val occurrences = mutableListOf<TaskEntity>()
        val ruleStart = try {
            LocalDate.parse(rule.startDate, dateFormatter)
        } catch (e: Exception) {
            fromDate
        }
        val ruleEnd = rule.endDate?.let {
            try { LocalDate.parse(it, dateFormatter) } catch (e: Exception) { null }
        }

        // The effective evaluation window
        val effectiveStart = if (fromDate.isBefore(ruleStart)) ruleStart else fromDate
        val effectiveEnd = if (ruleEnd != null && toDate.isAfter(ruleEnd)) ruleEnd else toDate

        if (effectiveStart.isAfter(effectiveEnd)) return emptyList()

        when (rule.type) {
            RecurrenceType.DAILY -> {
                var current = ruleStart
                while (!current.isAfter(effectiveEnd)) {
                    if (!current.isBefore(effectiveStart) && !rule.hasException(current.format(dateFormatter))) {
                        occurrences.add(createTaskForDate(baseTask, current, rule.id))
                    }
                    current = current.plusDays(rule.interval.coerceAtLeast(1).toLong())
                }
            }
            RecurrenceType.WEEKLY -> {
                var current = effectiveStart
                while (!current.isAfter(effectiveEnd)) {
                    val dayOfWeek = current.dayOfWeek.value // 1 = Monday .. 7 = Sunday
                    if (rule.isDayOfWeekSelected(dayOfWeek) && !rule.hasException(current.format(dateFormatter))) {
                        occurrences.add(createTaskForDate(baseTask, current, rule.id))
                    }
                    current = current.plusDays(1)
                }
            }
            RecurrenceType.MONTHLY_DATE -> {
                val targetDay = rule.dayOfMonth ?: ruleStart.dayOfMonth
                var currentMonth = YearMonth.from(effectiveStart)
                val endMonth = YearMonth.from(effectiveEnd)

                while (!currentMonth.isAfter(endMonth)) {
                    // Safe day clamping: handles 28, 29, 30, 31 properly
                    val safeDay = targetDay.coerceAtMost(currentMonth.lengthOfMonth())
                    val occurrenceDate = currentMonth.atDay(safeDay)

                    if (!occurrenceDate.isBefore(effectiveStart) &&
                        !occurrenceDate.isAfter(effectiveEnd) &&
                        !rule.hasException(occurrenceDate.format(dateFormatter))
                    ) {
                        occurrences.add(createTaskForDate(baseTask, occurrenceDate, rule.id))
                    }
                    currentMonth = currentMonth.plusMonths(rule.interval.coerceAtLeast(1).toLong())
                }
            }
            RecurrenceType.MONTHLY_DAY_OF_WEEK -> {
                // e.g. 1st Monday, 2nd Tuesday, last Friday (-1)
                val targetWeekday = DayOfWeek.of(rule.weekdayForNth?.coerceIn(1, 7) ?: 1)
                val nth = rule.nthWeekday ?: 1
                var currentMonth = YearMonth.from(effectiveStart)
                val endMonth = YearMonth.from(effectiveEnd)

                while (!currentMonth.isAfter(endMonth)) {
                    val occurrenceDate = if (nth == -1) {
                        currentMonth.atEndOfMonth().with(TemporalAdjusters.previousOrSame(targetWeekday))
                    } else {
                        currentMonth.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(nth, targetWeekday))
                    }

                    if (!occurrenceDate.isBefore(effectiveStart) &&
                        !occurrenceDate.isAfter(effectiveEnd) &&
                        !rule.hasException(occurrenceDate.format(dateFormatter))
                    ) {
                        occurrences.add(createTaskForDate(baseTask, occurrenceDate, rule.id))
                    }
                    currentMonth = currentMonth.plusMonths(rule.interval.coerceAtLeast(1).toLong())
                }
            }
            RecurrenceType.YEARLY -> {
                val targetMonth = rule.monthOfYear ?: ruleStart.monthValue
                val targetDay = rule.dayOfMonth ?: ruleStart.dayOfMonth
                var currentYear = effectiveStart.year

                while (currentYear <= effectiveEnd.year) {
                    val ym = YearMonth.of(currentYear, targetMonth)
                    val safeDay = targetDay.coerceAtMost(ym.lengthOfMonth())
                    val occurrenceDate = ym.atDay(safeDay)

                    if (!occurrenceDate.isBefore(effectiveStart) &&
                        !occurrenceDate.isAfter(effectiveEnd) &&
                        !rule.hasException(occurrenceDate.format(dateFormatter))
                    ) {
                        occurrences.add(createTaskForDate(baseTask, occurrenceDate, rule.id))
                    }
                    currentYear += rule.interval.coerceAtLeast(1)
                }
            }
            RecurrenceType.NONE -> {
                if (!ruleStart.isBefore(effectiveStart) && !ruleStart.isAfter(effectiveEnd)) {
                    occurrences.add(baseTask.copy(id = 0, recurrenceRuleId = null))
                }
            }
        }

        return occurrences
    }

    private fun createTaskForDate(base: TaskEntity, date: LocalDate, ruleId: Long): TaskEntity {
        return base.copy(
            id = 0,
            scheduledDate = date.format(dateFormatter),
            recurrenceRuleId = ruleId,
            status = TaskStatus.UPCOMING,
            actualCompletionTime = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Conflict Detection:
     * Checks if a new or updated task overlaps in time with any existing tasks on the same date.
     */
    fun detectConflicts(
        existingTasks: List<TaskEntity>,
        task: TaskEntity
    ): List<TaskEntity> {
        val newStart = parseTime(task.startTime) ?: return emptyList()
        val newEnd = newStart.plusMinutes(task.durationMinutes.toLong())

        return existingTasks.filter { other ->
            if (other.id == task.id || other.scheduledDate != task.scheduledDate || other.status == TaskStatus.SKIPPED) {
                false
            } else {
                val otherStart = parseTime(other.startTime)
                if (otherStart != null) {
                    val otherEnd = otherStart.plusMinutes(other.durationMinutes.toLong())
                    // Check time overlap: (StartA < EndB) and (EndA > StartB)
                    newStart.isBefore(otherEnd) && newEnd.isAfter(otherStart)
                } else {
                    false
                }
            }
        }
    }

    private fun parseTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            LocalTime.parse(timeStr, timeFormatter)
        } catch (e: Exception) {
            null
        }
    }
}
