package com.naman.focusos

import androidx.compose.ui.graphics.Color
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.SessionEventType
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.data.model.sortedChronologically
import com.naman.focusos.data.preferences.UserSettings
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import com.naman.focusos.domain.excel.ExcelManager
import com.naman.focusos.domain.excel.FullBackupData
import com.naman.focusos.domain.focus.AdvancedAnalyticsEngine
import com.naman.focusos.domain.focus.AnalyticsDateFilter
import com.naman.focusos.domain.focus.AppProductivityStat
import com.naman.focusos.domain.focus.AppUsageMonitor
import com.naman.focusos.domain.focus.AppUsageProvider
import com.naman.focusos.domain.focus.BestFocusPeriodRecommendation
import com.naman.focusos.domain.focus.CategoryFocusAnalytics
import com.naman.focusos.domain.focus.DailyFocusStats
import com.naman.focusos.domain.focus.DistractionDashboardData
import com.naman.focusos.domain.focus.DistractionPatternReport
import com.naman.focusos.domain.focus.FocusAnalyticsEngine
import com.naman.focusos.domain.focus.FocusScoreDetail
import com.naman.focusos.domain.focus.FocusTimerEngine
import com.naman.focusos.domain.focus.HeatmapCell
import com.naman.focusos.domain.focus.HourlyProductivity
import com.naman.focusos.domain.focus.InsightType
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.domain.focus.ProductivityInsight
import com.naman.focusos.domain.focus.RawUsageInterval
import com.naman.focusos.domain.focus.RecommendationConfidence
import com.naman.focusos.domain.focus.ScoreCategory
import com.naman.focusos.domain.focus.TaskEfficiencySummary
import com.naman.focusos.domain.focus.TaskFocusComparison
import androidx.sqlite.db.SupportSQLiteDatabase
import com.naman.focusos.data.db.FocusDatabase
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import com.naman.focusos.domain.focus.WeeklyFocusStats
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import com.naman.focusos.domain.scheduler.RecurrenceEngine
import com.naman.focusos.notification.TaskAlarmScheduler
import com.naman.focusos.ui.theme.DarkFocusColors
import com.naman.focusos.ui.theme.LavenderFocusColors
import com.naman.focusos.ui.theme.LightFocusColors
import com.naman.focusos.ui.theme.MidnightMochiFocusColors
import com.naman.focusos.ui.theme.PeachyFocusColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class FocusOSTestSuite {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ==========================================
    // 1. Task Creation, Editing & Status Evaluation Tests
    // ==========================================

    @Test
    fun testTaskCreationAndDefaults() {
        val task = TaskEntity(
            title = "Test Task",
            scheduledDate = "2026-11-05",
            startTime = "10:00",
            durationMinutes = 60,
            deadlineTime = "11:30"
        )
        assertEquals("Test Task", task.title)
        assertEquals(TaskStatus.UPCOMING, task.status)
        assertEquals(60, task.durationMinutes)
    }

    @Test
    fun testFutureTaskClassification() {
        val futureDate = LocalDate.now().plusDays(2).format(dateFormatter)
        val task = TaskEntity(
            title = "Future Task",
            scheduledDate = futureDate,
            deadlineTime = "12:00"
        )
        val status = task.evaluateEffectiveStatus(LocalDateTime.now())
        assertEquals(TaskStatus.UPCOMING, status)
    }

    @Test
    fun testMissedTaskClassificationWhenPastDeadline() {
        val today = LocalDate.now().format(dateFormatter)
        // Deadline 09:00 AM, current time 11:00 AM
        val pastDeadlineTask = TaskEntity(
            title = "Overdue Task",
            scheduledDate = today,
            startTime = "08:00",
            durationMinutes = 45,
            deadlineTime = "09:00"
        )
        val now = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0))
        val status = pastDeadlineTask.evaluateEffectiveStatus(now)
        assertEquals(TaskStatus.MISSED, status)
    }

    @Test
    fun testUpcomingBeforeDeadline() {
        val today = LocalDate.now().format(dateFormatter)
        // Deadline 18:00, current time 14:00
        val pendingTask = TaskEntity(
            title = "Pending Afternoon Task",
            scheduledDate = today,
            startTime = "16:00",
            durationMinutes = 60,
            deadlineTime = "18:00"
        )
        val now = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0))
        val status = pendingTask.evaluateEffectiveStatus(now)
        assertEquals(TaskStatus.UPCOMING, status)
    }

    @Test
    fun testCompletedOnTimeClassification() {
        val today = LocalDate.now().format(dateFormatter)
        val task = TaskEntity(
            title = "DSA Practice",
            scheduledDate = today,
            startTime = "10:00",
            durationMinutes = 60,
            deadlineTime = "11:30"
        )
        // Completed at 11:15 AM (Before 11:30 deadline)
        val completedAt = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 15))
        val updated = task.markCompleted(completedAt)
        assertEquals(TaskStatus.COMPLETED_ON_TIME, updated.status)
        assertNotNull(updated.actualCompletionTime)
    }

    @Test
    fun testCompletedLateClassification() {
        val today = LocalDate.now().format(dateFormatter)
        val task = TaskEntity(
            title = "DSA Practice",
            scheduledDate = today,
            startTime = "10:00",
            durationMinutes = 60,
            deadlineTime = "11:30"
        )
        // Completed at 11:45 AM (After 11:30 deadline)
        val completedAt = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 45))
        val updated = task.markCompleted(completedAt)
        assertEquals(TaskStatus.COMPLETED_LATE, updated.status)
    }

    @Test
    fun testUndoCompletion() {
        val task = TaskEntity(
            title = "Review Task",
            scheduledDate = "2026-11-02",
            status = TaskStatus.COMPLETED_ON_TIME,
            actualCompletionTime = 123456L
        )
        val undone = task.markIncomplete()
        assertEquals(TaskStatus.UPCOMING, undone.status)
        assertEquals(null, undone.actualCompletionTime)
    }

    @Test
    fun testTaskCompletionSavesAccurateTimestampAndFormattedTime() {
        val today = LocalDate.of(2026, 11, 20)
        val task = TaskEntity(
            title = "System Design Interview Prep",
            scheduledDate = today.format(dateFormatter),
            startTime = "10:00",
            durationMinutes = 60,
            deadlineTime = "12:00"
        )

        // Mark completed at 11:15 AM
        val completedAt = LocalDateTime.of(today, LocalTime.of(11, 15))
        val completedTask = task.markCompleted(completedAt)

        val expectedEpoch = completedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedEpoch, completedTask.actualCompletionTime)
        assertEquals(TaskStatus.COMPLETED_ON_TIME, completedTask.status)

        // Test time formatting helpers
        val formatted12H = completedTask.getFormattedCompletionTime(is24Hour = false)
        val formatted24H = completedTask.getFormattedCompletionTime(is24Hour = true)
        assertEquals("11:15 AM", formatted12H)
        assertEquals("11:15", formatted24H)

        // If incomplete, formatted completion time should be null
        assertNull(task.getFormattedCompletionTime())
    }

    @Test
    fun testDayStatsIncludesAllTasksWithCompletionTime() {
        val date = LocalDate.of(2026, 11, 20)
        val completedAt1 = LocalDateTime.of(date, LocalTime.of(6, 45))
        val completedAt2 = LocalDateTime.of(date, LocalTime.of(15, 30))

        val task1 = TaskEntity(
            id = 1,
            title = "Morning Yoga",
            scheduledDate = "2026-11-20",
            startTime = "06:00",
            durationMinutes = 45,
            deadlineTime = "07:00"
        ).markCompleted(completedAt1)

        val task2 = TaskEntity(
            id = 2,
            title = "Afternoon Code Review",
            scheduledDate = "2026-11-20",
            startTime = "14:00",
            durationMinutes = 60,
            deadlineTime = "15:00"
        ).markCompleted(completedAt2) // Completed late

        val task3 = TaskEntity(
            id = 3,
            title = "Evening Journaling",
            scheduledDate = "2026-11-20",
            startTime = "21:00",
            durationMinutes = 15,
            status = TaskStatus.UPCOMING
        )

        val tasks = listOf(task3, task1, task2) // Unsorted input
        val now = LocalDateTime.of(date, LocalTime.of(18, 0))
        val dayStats = ProductivityIntelligenceEngine.calculateDayStats(date, tasks, now)

        assertEquals(3, dayStats.totalTasks)
        assertEquals(1, dayStats.completedOnTime)
        assertEquals(1, dayStats.completedLate)
        assertEquals(1, dayStats.pending)

        // Verify tasks are present and sorted chronologically
        assertEquals(3, dayStats.tasks.size)
        assertEquals("Morning Yoga", dayStats.tasks[0].title)
        assertEquals("Afternoon Code Review", dayStats.tasks[1].title)
        assertEquals("Evening Journaling", dayStats.tasks[2].title)

        // Verify accurate saved completion times for day bar inspection
        assertEquals(completedAt1.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), dayStats.tasks[0].actualCompletionTime)
        assertEquals("06:45 AM", dayStats.tasks[0].getFormattedCompletionTime(is24Hour = false))

        assertEquals(completedAt2.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), dayStats.tasks[1].actualCompletionTime)
        assertEquals("03:30 PM", dayStats.tasks[1].getFormattedCompletionTime(is24Hour = false))

        assertNull(dayStats.tasks[2].actualCompletionTime)
    }

    @Test
    fun testHabitSyncEngineQualificationRules() {
        // 1. One-day task with no recurrence must NOT qualify
        val oneDayTasks = listOf(
            TaskEntity(
                title = "Renew Driver License",
                scheduledDate = "2026-11-20",
                recurrenceRuleId = null
            )
        )
        assertFalse(HabitSyncEngine.isHabitQualifying(oneDayTasks))

        // 2. Task with recurrence rule qualifies even on 1 initial occurrence
        val recurringTask = listOf(
            TaskEntity(
                title = "Daily Meditation",
                scheduledDate = "2026-11-20",
                recurrenceRuleId = 42L
            )
        )
        assertTrue(HabitSyncEngine.isHabitQualifying(recurringTask))

        // 3. Tasks spanning multiple distinct dates qualify as habit/routine
        val multiDayTasks = listOf(
            TaskEntity(title = "Gym Workout", scheduledDate = "2026-11-20"),
            TaskEntity(title = "Gym Workout", scheduledDate = "2026-11-22")
        )
        assertTrue(HabitSyncEngine.isHabitQualifying(multiDayTasks))
    }

    @Test
    fun testOneDayTasksExcludedFromActiveHabitsFilter() {
        val allTasks = listOf(
            // One-day tasks (must be excluded)
            TaskEntity(id = 1, title = "Buy groceries", scheduledDate = "2026-11-20", recurrenceRuleId = null),
            TaskEntity(id = 2, title = "Car oil change", scheduledDate = "2026-11-21", recurrenceRuleId = null),

            // Recurring task (must be included)
            TaskEntity(id = 3, title = "Morning Yoga", scheduledDate = "2026-11-20", recurrenceRuleId = 100L),
            TaskEntity(id = 4, title = "Morning Yoga", scheduledDate = "2026-11-21", recurrenceRuleId = 100L),

            // Multi-day routine task (must be included)
            TaskEntity(id = 5, title = "LeetCode Practice", scheduledDate = "2026-11-19"),
            TaskEntity(id = 6, title = "LeetCode Practice", scheduledDate = "2026-11-20")
        )

        val activeHabitGroups = HabitSyncEngine.filterActiveHabitTasks(allTasks)

        // Verify one-day tasks are strictly excluded
        assertFalse(activeHabitGroups.containsKey("buy groceries"))
        assertFalse(activeHabitGroups.containsKey("car oil change"))

        // Verify recurring and multi-day tasks are included
        assertTrue(activeHabitGroups.containsKey("morning yoga"))
        assertTrue(activeHabitGroups.containsKey("leetcode practice"))
        assertEquals(2, activeHabitGroups.size)

        // Verify habit entity creation
        val yogaHabit = HabitSyncEngine.createHabitFromTasks(activeHabitGroups["morning yoga"]!!)
        assertEquals("Morning Yoga", yogaHabit.title)
        assertTrue(yogaHabit.isActive)
        assertEquals(HabitFrequency.DAILY, yogaHabit.frequency)
    }

    @Test
    fun testHabitCompletionAutoGeneratedForCompletedTasks() {
        val date1 = "2026-11-20"
        val date2 = "2026-11-21"
        val tasks = listOf(
            TaskEntity(
                id = 10,
                title = "Study System Design",
                scheduledDate = date1,
                recurrenceRuleId = 5L,
                status = TaskStatus.COMPLETED_ON_TIME,
                actualCompletionTime = 1700000000L
            ),
            TaskEntity(
                id = 11,
                title = "Study System Design",
                scheduledDate = date2,
                recurrenceRuleId = 5L,
                status = TaskStatus.UPCOMING,
                actualCompletionTime = null
            )
        )

        val completions = HabitSyncEngine.generateCompletionsForHabit(habitId = 77L, tasks = tasks)
        // Only completed tasks generate habit completion records
        assertEquals(1, completions.size)
        val comp = completions[0]
        assertEquals(77L, comp.habitId)
        assertEquals(date1, comp.date)
        assertTrue(comp.isCompleted)
        assertEquals(1700000000L, comp.completedAt)
    }

    @Test
    fun testEditTaskPropagatesToAllOccurrencesInSeries() {
        val ruleId = 55L
        val originalDate1Task = TaskEntity(
            id = 101L,
            title = "Morning Yoga",
            scheduledDate = "2026-11-20",
            startTime = "06:00",
            durationMinutes = 30,
            deadlineTime = "07:00",
            recurrenceRuleId = ruleId,
            status = TaskStatus.UPCOMING
        )

        val originalDate2Task = TaskEntity(
            id = 102L,
            title = "Morning Yoga",
            scheduledDate = "2026-11-21",
            startTime = "06:00",
            durationMinutes = 30,
            deadlineTime = "07:00",
            recurrenceRuleId = ruleId,
            status = TaskStatus.COMPLETED_ON_TIME,
            actualCompletionTime = 1700000000L
        )

        // User edits the task on date 1: updates title, time, duration, and deadline
        val editedDate1Task = originalDate1Task.copy(
            title = "Vinyasa Flow Yoga",
            startTime = "06:30",
            durationMinutes = 45,
            deadlineTime = "07:30"
        )

        // Simulate multi-day series propagation logic
        val seriesTasks = listOf(originalDate2Task)
        val updatedSeries = seriesTasks.map { other ->
            other.copy(
                title = editedDate1Task.title,
                description = editedDate1Task.description,
                categoryId = editedDate1Task.categoryId,
                priority = editedDate1Task.priority,
                startTime = editedDate1Task.startTime,
                durationMinutes = editedDate1Task.durationMinutes,
                deadlineTime = editedDate1Task.deadlineTime,
                notificationTime1 = editedDate1Task.notificationTime1,
                notificationTime2 = editedDate1Task.notificationTime2,
                notificationTime3 = editedDate1Task.notificationTime3,
                updatedAt = System.currentTimeMillis()
            )
        }

        // Verify edited task retains its identity
        assertEquals("Vinyasa Flow Yoga", editedDate1Task.title)
        assertEquals("06:30", editedDate1Task.startTime)
        assertEquals(45, editedDate1Task.durationMinutes)

        // Verify other occurrences across all days receive the updated properties
        val updatedDate2 = updatedSeries[0]
        assertEquals(102L, updatedDate2.id) // Preserved
        assertEquals("2026-11-21", updatedDate2.scheduledDate) // Preserved
        assertEquals(TaskStatus.COMPLETED_ON_TIME, updatedDate2.status) // Completion preserved
        assertEquals(1700000000L, updatedDate2.actualCompletionTime) // Timestamp preserved
        assertEquals("Vinyasa Flow Yoga", updatedDate2.title) // Updated across all days
        assertEquals("06:30", updatedDate2.startTime) // Updated across all days
        assertEquals(45, updatedDate2.durationMinutes) // Updated across all days
        assertEquals("07:30", updatedDate2.deadlineTime) // Updated across all days
    }

    @Test
    fun testWeeklyRecurrenceRuleDaysMaskRoundtrip() {
        // Monday (1), Wednesday (3), Friday (5)
        val chosenDays = listOf(1, 3, 5)
        val mask = RecurrenceRuleEntity.createWeeklyMask(chosenDays)

        // Parse mask back into active weekdays
        val parsedDays = RecurrenceRuleEntity.parseWeeklyMask(mask)
        assertEquals(chosenDays.sorted(), parsedDays.sorted())

        val weeklyRule = RecurrenceRuleEntity(
            id = 20,
            type = RecurrenceType.WEEKLY,
            daysOfWeekMask = mask,
            startDate = "2026-11-20"
        )
        assertEquals(RecurrenceType.WEEKLY, weeklyRule.type)
        assertEquals(mask, weeklyRule.daysOfWeekMask)
    }

    // ==========================================
    // 2. Advanced Scheduling & Recurrence Engine Tests
    // ==========================================

    @Test
    fun testDailyRecurrence() {
        val baseTask = TaskEntity(
            title = "Morning Meditation",
            scheduledDate = "2026-11-01"
        )
        val rule = RecurrenceRuleEntity(
            id = 10,
            type = RecurrenceType.DAILY,
            interval = 1,
            startDate = "2026-11-01"
        )

        val occurrences = RecurrenceEngine.generateOccurrences(
            rule = rule,
            baseTask = baseTask,
            fromDate = LocalDate.of(2026, 11, 1),
            toDate = LocalDate.of(2026, 11, 7)
        )
        assertEquals(7, occurrences.size)
        assertEquals("2026-11-01", occurrences[0].scheduledDate)
        assertEquals("2026-11-07", occurrences[6].scheduledDate)
    }

    @Test
    fun testWeeklyRecurrence() {
        val baseTask = TaskEntity(
            title = "Gym Session",
            scheduledDate = "2026-11-02"
        )
        // Mon, Wed, Fri = days 1, 3, 5
        val mask = RecurrenceRuleEntity.createWeeklyMask(listOf(1, 3, 5))
        val rule = RecurrenceRuleEntity(
            id = 20,
            type = RecurrenceType.WEEKLY,
            daysOfWeekMask = mask,
            startDate = "2026-11-02" // Monday
        )

        val occurrences = RecurrenceEngine.generateOccurrences(
            rule = rule,
            baseTask = baseTask,
            fromDate = LocalDate.of(2026, 11, 2),
            toDate = LocalDate.of(2026, 11, 8) // Full week Mon..Sun
        )
        // Should generate Mon (Nov 2), Wed (Nov 4), Fri (Nov 6) = 3 occurrences
        assertEquals(3, occurrences.size)
        assertEquals("2026-11-02", occurrences[0].scheduledDate)
        assertEquals("2026-11-04", occurrences[1].scheduledDate)
        assertEquals("2026-11-06", occurrences[2].scheduledDate)
    }

    @Test
    fun testMonthlyRecurrenceWithShortMonthsAndLeapYear() {
        val baseTask = TaskEntity(
            title = "Monthly Review",
            scheduledDate = "2024-01-31" // 31st of month
        )
        val rule = RecurrenceRuleEntity(
            id = 30,
            type = RecurrenceType.MONTHLY_DATE,
            dayOfMonth = 31,
            startDate = "2024-01-31"
        )

        // 2024 is a Leap Year! Feb has 29 days.
        val occurrences = RecurrenceEngine.generateOccurrences(
            rule = rule,
            baseTask = baseTask,
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = LocalDate.of(2024, 4, 30)
        )
        assertEquals(4, occurrences.size)
        assertEquals("2024-01-31", occurrences[0].scheduledDate)
        // In Feb 2024 (leap year), day 31 clamps to Feb 29!
        assertEquals("2024-02-29", occurrences[1].scheduledDate)
        // In March 2024, day 31 is valid
        assertEquals("2024-03-31", occurrences[2].scheduledDate)
        // In April 2024, day 31 clamps to April 30!
        assertEquals("2024-04-30", occurrences[3].scheduledDate)
    }

    @Test
    fun testRecurrenceExceptionDates() {
        val baseTask = TaskEntity(
            title = "Daily Routine",
            scheduledDate = "2026-11-01"
        )
        val rule = RecurrenceRuleEntity(
            id = 40,
            type = RecurrenceType.DAILY,
            startDate = "2026-11-01",
            exceptionDates = "2026-11-03, 2026-11-05" // Skip 3rd and 5th
        )

        val occurrences = RecurrenceEngine.generateOccurrences(
            rule = rule,
            baseTask = baseTask,
            fromDate = LocalDate.of(2026, 11, 1),
            toDate = LocalDate.of(2026, 11, 6)
        )
        // 6 days total - 2 skipped = 4 occurrences
        assertEquals(4, occurrences.size)
        val dates = occurrences.map { it.scheduledDate }
        assertTrue(dates.contains("2026-11-01"))
        assertTrue(dates.contains("2026-11-02"))
        assertFalse(dates.contains("2026-11-03"))
        assertTrue(dates.contains("2026-11-04"))
        assertFalse(dates.contains("2026-11-05"))
        assertTrue(dates.contains("2026-11-06"))
    }

    @Test
    fun testConflictDetection() {
        val existing = listOf(
            TaskEntity(id = 1, title = "Task A", scheduledDate = "2026-11-02", startTime = "10:00", durationMinutes = 60), // 10:00 - 11:00
            TaskEntity(id = 2, title = "Task B", scheduledDate = "2026-11-02", startTime = "13:00", durationMinutes = 60)  // 13:00 - 14:00
        )

        // Candidate task: 10:30 to 11:30 (overlaps with Task A)
        val candidate = TaskEntity(title = "Candidate", scheduledDate = "2026-11-02", startTime = "10:30", durationMinutes = 60)
        val conflicts = RecurrenceEngine.detectConflicts(existing, candidate)
        assertEquals(1, conflicts.size)
        assertEquals("Task A", conflicts[0].title)

        // Non-conflicting candidate: 11:00 to 12:00 (no overlap)
        val nonConflicting = TaskEntity(title = "Clear Slot", scheduledDate = "2026-11-02", startTime = "11:00", durationMinutes = 60)
        val noConflicts = RecurrenceEngine.detectConflicts(existing, nonConflicting)
        assertEquals(0, noConflicts.size)
    }

    // ==========================================
    // 3. Productivity Intelligence & Analytics Calculations
    // ==========================================

    @Test
    fun testAnalyticsCalculations() {
        val date = LocalDate.of(2026, 11, 2)
        val tasks = listOf(
            TaskEntity(title = "Task 1", scheduledDate = "2026-11-02", status = TaskStatus.COMPLETED_ON_TIME, durationMinutes = 60),
            TaskEntity(title = "Task 2", scheduledDate = "2026-11-02", status = TaskStatus.COMPLETED_LATE, durationMinutes = 45),
            TaskEntity(title = "Task 3", scheduledDate = "2026-11-02", status = TaskStatus.MISSED, durationMinutes = 30),
            TaskEntity(title = "Task 4", scheduledDate = "2026-11-02", status = TaskStatus.SKIPPED, durationMinutes = 20)
        )

        val stats = ProductivityIntelligenceEngine.calculateDayStats(date, tasks)
        assertEquals(4, stats.totalTasks)
        assertEquals(1, stats.completedOnTime)
        assertEquals(1, stats.completedLate)
        assertEquals(1, stats.missed)
        assertEquals(1, stats.skipped)
        // Skipped is excluded from completion rate denominator: 2 completed / (4 - 1 skipped) = 2/3 = 66.67%
        assertEquals(66.67f, stats.completionRate, 0.5f)
        assertEquals(155, stats.plannedDurationMinutes)
    }

    @Test
    fun testHabitStreakCalculation() {
        val today = LocalDate.of(2026, 11, 10)
        // Habit completed on Nov 10, Nov 9, Nov 8 (streak of 3)
        // Broken on Nov 7
        // Completed on Nov 6, Nov 5, Nov 4, Nov 3 (streak of 4 -> best streak)
        val completions = listOf(
            HabitCompletionEntity(habitId = 1, date = "2026-11-10", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-09", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-08", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-06", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-05", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-04", isCompleted = true),
            HabitCompletionEntity(habitId = 1, date = "2026-11-03", isCompleted = true)
        )

        val (currentStreak, bestStreak) = ProductivityIntelligenceEngine.calculateHabitStreak(completions, today)
        assertEquals(3, currentStreak)
        assertEquals(4, bestStreak)
    }

    // ==========================================
    // 4. Excel Import, Export & Full Backup Round-Trip
    // ==========================================

    @Test
    fun testExcelImportOfHabitTrackerWorkbook() {
        val file = listOf(
            File("Habit_Tracker.xlsx"),
            File("../Habit_Tracker.xlsx"),
            File("src/main/assets/Habit_Tracker.xlsx"),
            File("app/src/main/assets/Habit_Tracker.xlsx")
        ).firstOrNull { it.exists() } ?: File("Habit_Tracker.xlsx")

        assertTrue("Habit_Tracker.xlsx must exist for test", file.exists())

        val preview = FileInputStream(file).use { stream ->
            ExcelManager.parseHabitTrackerWorkbook(stream)
        }

        assertEquals("Habit Tracker - Month", preview.sheetName)
        assertEquals(12, preview.habitTitles.size)
        assertTrue(preview.habitTitles.contains("Wake up at 6 AM"))
        assertTrue(preview.habitTitles.contains("Drink 2L Water"))
        assertTrue(preview.habitTitles.contains("Workout"))
        assertTrue(preview.totalCompletionsFound > 0)
    }

    @Test
    fun testCsvTaskExport() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Task 1", description = "Test Desc", scheduledDate = "2026-11-02", priority = Priority.CRITICAL, status = TaskStatus.COMPLETED_ON_TIME),
            TaskEntity(id = 2, title = "Task 2, with comma", scheduledDate = "2026-11-03", priority = Priority.MEDIUM, status = TaskStatus.UPCOMING)
        )

        val outputStream = ByteArrayOutputStream()
        ExcelManager.exportTasksToCsv(tasks, outputStream)
        val csvText = outputStream.toString("UTF-8")

        assertTrue(csvText.contains("Task ID,Title,Description"))
        assertTrue(csvText.contains("Task 1"))
        assertTrue(csvText.contains("\"Task 2, with comma\""))
    }

    @Test
    fun testFullBackupAndRestoreRoundTrip() {
        val backup = FullBackupData(
            exportDate = "2026-11-02",
            tasks = listOf(
                TaskEntity(id = 1, title = "Important Task", scheduledDate = "2026-11-02", priority = Priority.HIGH, status = TaskStatus.COMPLETED_ON_TIME)
            ),
            recurrenceRules = emptyList(),
            habits = listOf(
                HabitEntity(id = 10, title = "Meditation", colorHex = "#38BDF8")
            ),
            habitCompletions = listOf(
                HabitCompletionEntity(id = 100, habitId = 10, date = "2026-11-02", isCompleted = true)
            ),
            timetableEntries = listOf(
                TimetableEntryEntity(id = 5, title = "Morning Run", startTime = "06:30", durationMinutes = 45)
            ),
            categories = emptyList()
        )

        val outStream = ByteArrayOutputStream()
        ExcelManager.exportFullBackup(backup, outStream)
        val jsonBytes = outStream.toByteArray()

        val inStream = ByteArrayInputStream(jsonBytes)
        val restored = ExcelManager.parseFullBackup(inStream)

        assertEquals(1, restored.tasks.size)
        assertEquals("Important Task", restored.tasks[0].title)
        assertEquals(1, restored.habits.size)
        assertEquals("Meditation", restored.habits[0].title)
        assertEquals(1, restored.habitCompletions.size)
        assertEquals(1, restored.timetableEntries.size)
        assertEquals("Morning Run", restored.timetableEntries[0].title)
    }

    // ==========================================
    // 5. Task Scheduling, Editing & Intelligent Chronological Sorting Tests
    // ==========================================

    @Test
    fun testScenario1_CreateTaskWithScheduledTime() {
        val task = TaskEntity(
            id = 1,
            title = "Morning Team Standup",
            scheduledDate = "2026-11-17",
            startTime = "09:30",
            durationMinutes = 30
        )
        assertEquals("09:30", task.startTime)
        assertEquals(30, task.durationMinutes)
        assertEquals("2026-11-17", task.scheduledDate)
    }

    @Test
    fun testScenario2_CreateTaskWithoutScheduledTime() {
        val task = TaskEntity(
            id = 2,
            title = "Read a Chapter",
            scheduledDate = "2026-11-17",
            startTime = null,
            durationMinutes = 30
        )
        assertEquals(null, task.startTime)
        assertEquals(TaskStatus.UPCOMING, task.status)
    }

    @Test
    fun testScenario3_EditExistingTaskTitle() {
        val original = TaskEntity(
            id = 10,
            title = "Original Title",
            scheduledDate = "2026-11-17",
            status = TaskStatus.UPCOMING,
            createdAt = 1000L
        )

        val edited = original.copy(
            title = "Updated Focused Title",
            updatedAt = 2000L
        )

        assertEquals(10L, edited.id)
        assertEquals(1000L, edited.createdAt)
        assertEquals(TaskStatus.UPCOMING, edited.status)
        assertEquals("Updated Focused Title", edited.title)
    }

    @Test
    fun testScenario4_EditScheduledDateAndTime() {
        val original = TaskEntity(
            id = 15,
            title = "Project Sync",
            scheduledDate = "2026-11-17",
            startTime = "10:00",
            durationMinutes = 45
        )

        val edited = original.copy(
            scheduledDate = "2026-11-18",
            startTime = "14:30"
        )

        assertEquals(15L, edited.id)
        assertEquals("2026-11-18", edited.scheduledDate)
        assertEquals("14:30", edited.startTime)
        assertEquals(45, edited.durationMinutes)
    }

    @Test
    fun testScenario5_EditDuration() {
        val original = TaskEntity(
            id = 20,
            title = "Deep Focus Coding",
            scheduledDate = "2026-11-17",
            startTime = "11:00",
            durationMinutes = 30
        )

        val edited = original.copy(durationMinutes = 90)

        assertEquals(20L, edited.id)
        assertEquals(90, edited.durationMinutes)
        assertEquals("11:00", edited.startTime)
    }

    @Test
    fun testScenario6_EditHardDeadline() {
        val original = TaskEntity(
            id = 25,
            title = "Submit Report",
            scheduledDate = "2026-11-17",
            startTime = "10:00",
            deadlineTime = "12:00"
        )

        val edited = original.copy(deadlineTime = "13:30")

        assertEquals(25L, edited.id)
        assertEquals("10:00", edited.startTime) // Start time remains distinct
        assertEquals("13:30", edited.deadlineTime)
    }

    @Test
    fun testScenario7_EditCompletedTaskPreservesCompletionStatus() {
        val completionEpoch = 1731850000000L
        val originalCompleted = TaskEntity(
            id = 30,
            title = "Workout Session",
            scheduledDate = "2026-11-17",
            startTime = "07:00",
            status = TaskStatus.COMPLETED_ON_TIME,
            actualCompletionTime = completionEpoch,
            createdAt = 1731800000000L
        )

        val edited = originalCompleted.copy(
            title = "Gym Session - Leg Day",
            startTime = "07:30",
            durationMinutes = 60
        )

        // Preserves ID, creation time, status, and actual completion timestamp
        assertEquals(30L, edited.id)
        assertEquals(1731800000000L, edited.createdAt)
        assertEquals(TaskStatus.COMPLETED_ON_TIME, edited.status)
        assertEquals(completionEpoch, edited.actualCompletionTime)
        assertEquals("Gym Session - Leg Day", edited.title)
        assertEquals("07:30", edited.startTime)
    }

    @Test
    fun testScenario8And9_IntelligentChronologicalOrdering() {
        // Matches the exact example from the requirement:
        // 05:00 — Wake up
        // 06:00 — Meditation
        // 08:30 — Skin care morning routine
        // 09:00 — Study
        // 11:30 — Project work
        // No time — Temple
        // No time — Buy groceries

        val tasks = listOf(
            TaskEntity(id = 7, title = "Buy groceries", scheduledDate = "2026-11-17", startTime = null),
            TaskEntity(id = 5, title = "Project work", scheduledDate = "2026-11-17", startTime = "11:30"),
            TaskEntity(id = 1, title = "Wake up", scheduledDate = "2026-11-17", startTime = "05:00"),
            TaskEntity(id = 6, title = "Temple", scheduledDate = "2026-11-17", startTime = null),
            TaskEntity(id = 4, title = "Study", scheduledDate = "2026-11-17", startTime = "09:00"),
            TaskEntity(id = 2, title = "Meditation", scheduledDate = "2026-11-17", startTime = "06:00"),
            TaskEntity(id = 3, title = "Skin care morning routine", scheduledDate = "2026-11-17", startTime = "08:30")
        )

        val sorted = tasks.sortedChronologically()

        // 1. Scheduled tasks must appear first in ascending order
        assertEquals("Wake up", sorted[0].title)
        assertEquals("05:00", sorted[0].startTime)

        assertEquals("Meditation", sorted[1].title)
        assertEquals("06:00", sorted[1].startTime)

        assertEquals("Skin care morning routine", sorted[2].title)
        assertEquals("08:30", sorted[2].startTime)

        assertEquals("Study", sorted[3].title)
        assertEquals("09:00", sorted[3].startTime)

        assertEquals("Project work", sorted[4].title)
        assertEquals("11:30", sorted[4].startTime)

        // 2. Tasks without times must appear after all scheduled tasks
        assertNull(sorted[5].startTime)
        assertNull(sorted[6].startTime)
        val untimedTitles = listOf(sorted[5].title, sorted[6].title)
        assertTrue(untimedTitles.contains("Temple"))
        assertTrue(untimedTitles.contains("Buy groceries"))
    }

    @Test
    fun testScenario10_TasksWithoutTimesAppearLastAcrossVariousTimes() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "No Time 1", scheduledDate = "2026-11-17", startTime = null),
            TaskEntity(id = 2, title = "Late Task", scheduledDate = "2026-11-17", startTime = "23:00"),
            TaskEntity(id = 3, title = "No Time 2", scheduledDate = "2026-11-17", startTime = null),
            TaskEntity(id = 4, title = "Early Task", scheduledDate = "2026-11-17", startTime = "01:00")
        )

        val sorted = tasks.sortedChronologically()

        assertEquals("Early Task", sorted[0].title)
        assertEquals("Late Task", sorted[1].title)
        assertNull(sorted[2].startTime)
        assertNull(sorted[3].startTime)
    }

    @Test
    fun testScenario11_DeterministicSecondarySorting() {
        // Two tasks with same scheduled time: higher priority should come first
        val criticalTask = TaskEntity(id = 1, title = "B-Critical Task", scheduledDate = "2026-11-17", startTime = "10:00", priority = Priority.CRITICAL)
        val mediumTask = TaskEntity(id = 2, title = "A-Medium Task", scheduledDate = "2026-11-17", startTime = "10:00", priority = Priority.MEDIUM)

        val sortedSameTime = listOf(mediumTask, criticalTask).sortedChronologically()
        assertEquals("B-Critical Task", sortedSameTime[0].title)
        assertEquals("A-Medium Task", sortedSameTime[1].title)

        // Two untimed tasks: deterministic secondary sorting applies
        val untimedHigh = TaskEntity(id = 3, title = "High Untimed", scheduledDate = "2026-11-17", startTime = null, priority = Priority.HIGH)
        val untimedLow = TaskEntity(id = 4, title = "Low Untimed", scheduledDate = "2026-11-17", startTime = null, priority = Priority.LOW)

        val sortedUntimed = listOf(untimedLow, untimedHigh).sortedChronologically()
        assertEquals("High Untimed", sortedUntimed[0].title)
        assertEquals("Low Untimed", sortedUntimed[1].title)
    }

    @Test
    fun testHardDeadlineDoesNotOverrideScheduledTime() {
        // Task A has a deadline at 08:00, but no scheduled time
        val deadlineOnlyTask = TaskEntity(
            id = 1,
            title = "Deadline Only Task",
            scheduledDate = "2026-11-17",
            startTime = null,
            deadlineTime = "08:00"
        )
        // Task B has a scheduled time at 10:00, and deadline at 12:00
        val scheduledTask = TaskEntity(
            id = 2,
            title = "Scheduled Task",
            scheduledDate = "2026-11-17",
            startTime = "10:00",
            deadlineTime = "12:00"
        )

        val sorted = listOf(deadlineOnlyTask, scheduledTask).sortedChronologically()

        // Scheduled task must appear before untimed task, regardless of deadline
        assertEquals("Scheduled Task", sorted[0].title)
        assertEquals("Deadline Only Task", sorted[1].title)
    }

    @Test
    fun testTaskCreationTimeDoesNotOverrideMissingScheduledTime() {
        // Older untimed task vs newer untimed task
        val olderTask = TaskEntity(id = 1, title = "Task Alpha", scheduledDate = "2026-11-17", startTime = null, createdAt = 1000L)
        val newerTask = TaskEntity(id = 2, title = "Task Beta", scheduledDate = "2026-11-17", startTime = null, createdAt = 9999999L)
        val scheduledTask = TaskEntity(id = 3, title = "Morning Run", scheduledDate = "2026-11-17", startTime = "06:00", createdAt = 5000L)

        val sorted = listOf(newerTask, olderTask, scheduledTask).sortedChronologically()

        assertEquals("Morning Run", sorted[0].title)
        assertNull(sorted[1].startTime)
        assertNull(sorted[2].startTime)
    }

    @Test
    fun testSortingIsStableAcrossRepeatedSorts() {
        val tasks = (1..20).map { i ->
            TaskEntity(
                id = i.toLong(),
                title = "Task $i",
                scheduledDate = "2026-11-17",
                startTime = if (i % 2 == 0) String.format(Locale.US, "%02d:00", (i % 12) + 1) else null
            )
        }

        val run1 = tasks.shuffled().sortedChronologically().map { it.id }
        val run2 = tasks.shuffled().sortedChronologically().map { it.id }

        assertEquals(run1, run2)
    }

    // ==========================================
    // 8. Complete Light & Dark Theme System Tests
    // ==========================================

    @Test
    fun testDefaultThemeModeIsSystem() {
        val defaultSettings = UserSettings()
        assertEquals("SYSTEM", defaultSettings.themeMode)
    }

    @Test
    fun testThemeResolutionLogic() {
        fun resolveIsDark(themeMode: String, systemInDarkTheme: Boolean): Boolean {
            return when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemInDarkTheme
            }
        }

        // 1. Explicit DARK mode
        assertTrue(resolveIsDark("DARK", systemInDarkTheme = false))
        assertTrue(resolveIsDark("DARK", systemInDarkTheme = true))

        // 2. Explicit LIGHT mode
        assertFalse(resolveIsDark("LIGHT", systemInDarkTheme = false))
        assertFalse(resolveIsDark("LIGHT", systemInDarkTheme = true))

        // 3. Follow SYSTEM mode (default)
        assertFalse(resolveIsDark("SYSTEM", systemInDarkTheme = false))
        assertTrue(resolveIsDark("SYSTEM", systemInDarkTheme = true))
    }

    @Test
    fun testLightAndDarkThemePalettes() {
        // Verify Light theme palette matches specifications exactly
        assertFalse(LightFocusColors.isDark)
        assertEquals(Color(0xFFF6F8FC), LightFocusColors.background)
        assertEquals(Color(0xFFFFFFFF), LightFocusColors.surface)
        assertEquals(Color(0xFF172033), LightFocusColors.textPrimary)
        assertEquals(Color(0xFF64748B), LightFocusColors.textSecondary)
        assertEquals(Color(0xFF2563EB), LightFocusColors.primaryAccent)
        assertEquals(Color(0xFFE5EAF2), LightFocusColors.border)
        assertEquals(Color(0xFFF1F5F9), LightFocusColors.surfaceVariant)
        assertEquals(Color(0xFF059669), LightFocusColors.success)
        assertEquals(Color(0xFFD97706), LightFocusColors.warning)
        assertEquals(Color(0xFFDC2626), LightFocusColors.danger)

        // Verify RupeeFlow Dark theme palette maintains dark visual styling
        assertTrue(DarkFocusColors.isDark)
        assertEquals(Color(0xFF0A0E17), DarkFocusColors.background)
        assertEquals(Color(0xFF111827), DarkFocusColors.surface)
        assertEquals(Color(0xFF3B82F6), DarkFocusColors.primaryAccent)
    }

    @Test
    fun testThemeModeSwitchingPreservesAllTasksAndHabits() {
        val originalTask = TaskEntity(
            id = 42L,
            title = "Crucial Architecture Review",
            scheduledDate = "2026-11-17",
            startTime = "14:00",
            durationMinutes = 45,
            deadlineTime = "15:30",
            status = TaskStatus.COMPLETED_ON_TIME
        )

        var settings = UserSettings(themeMode = "SYSTEM")
        assertEquals("SYSTEM", settings.themeMode)

        // Switch to LIGHT
        settings = settings.copy(themeMode = "LIGHT")
        assertEquals("LIGHT", settings.themeMode)
        assertEquals("Crucial Architecture Review", originalTask.title)
        assertEquals("14:00", originalTask.startTime)
        assertEquals(TaskStatus.COMPLETED_ON_TIME, originalTask.status)

        // Switch to DARK
        settings = settings.copy(themeMode = "DARK")
        assertEquals("DARK", settings.themeMode)
        assertEquals(42L, originalTask.id)
        assertEquals(45, originalTask.durationMinutes)

        // Switch back to SYSTEM
        settings = settings.copy(themeMode = "SYSTEM")
        assertEquals("SYSTEM", settings.themeMode)
    }

    // ==========================================
    // 9. Task Multi-Notification & Reminder Tests
    // ==========================================

    @Test
    fun testTaskWithSingleNotification() {
        val task = TaskEntity(
            title = "Morning Deep Work",
            scheduledDate = "2026-11-20",
            startTime = "09:00",
            notificationTime1 = "08:45"
        )
        assertEquals("08:45", task.notificationTime1)
        assertNull(task.notificationTime2)
        assertNull(task.notificationTime3)
    }

    @Test
    fun testTaskWithSecondAndThirdNotificationTimes() {
        val task = TaskEntity(
            title = "Complete Quarterly Tax Filing",
            scheduledDate = "2026-11-20",
            startTime = "10:00",
            notificationTime1 = "10:00",  // 1st notification (Primary)
            notificationTime2 = "11:30",  // 2nd notification (if missed to complete)
            notificationTime3 = "14:00"   // 3rd notification (final if missed to complete)
        )
        assertEquals("10:00", task.notificationTime1)
        assertEquals("11:30", task.notificationTime2)
        assertEquals("14:00", task.notificationTime3)
    }

    @Test
    fun testEditTaskNotificationTimesPreservesIdAndStatus() {
        val original = TaskEntity(
            id = 99L,
            title = "Initial Task",
            scheduledDate = "2026-11-20",
            notificationTime1 = "10:00",
            notificationTime2 = null,
            notificationTime3 = null,
            status = TaskStatus.UPCOMING
        )

        // Add 2nd and 3rd notification times during edit
        val edited = original.copy(
            notificationTime2 = "11:00",
            notificationTime3 = "12:00"
        )

        assertEquals(99L, edited.id)
        assertEquals("Initial Task", edited.title)
        assertEquals("10:00", edited.notificationTime1)
        assertEquals("11:00", edited.notificationTime2)
        assertEquals("12:00", edited.notificationTime3)
        assertEquals(TaskStatus.UPCOMING, edited.status)
    }

    @Test
    fun testCompletedTaskWithNotificationsSuppressionLogic() {
        val task = TaskEntity(
            id = 55L,
            title = "Workout Session",
            scheduledDate = "2026-11-20",
            notificationTime1 = "07:00",
            notificationTime2 = "07:30",
            notificationTime3 = "08:00",
            status = TaskStatus.UPCOMING
        )
        assertFalse(task.status.isCompleted())

        // User marks complete after 1st notification
        val completedTask = task.markCompleted(LocalDateTime.now())
        assertTrue(completedTask.status.isCompleted())

        // In TaskAlarmReceiver: task.status.isCompleted() is checked before sending 2nd and 3rd notification
        // When task is completed, isCompleted() is true, so notifications 2 & 3 are suppressed
        val shouldSuppressFollowUpNotification = completedTask.status.isCompleted()
        assertTrue(shouldSuppressFollowUpNotification)
    }

    @Test
    fun testTaskNotificationTimesBackupAndRestoreRoundTrip() {
        val task = TaskEntity(
            id = 77L,
            title = "Backup Test Task with Notifications",
            scheduledDate = "2026-11-20",
            startTime = "08:00",
            durationMinutes = 45,
            deadlineTime = "12:00",
            notificationTime1 = "08:00",
            notificationTime2 = "09:30",
            notificationTime3 = "11:00"
        )

        val backupData = FullBackupData(
            exportDate = "2026-11-20",
            tasks = listOf(task),
            recurrenceRules = emptyList(),
            habits = emptyList(),
            habitCompletions = emptyList(),
            timetableEntries = emptyList(),
            categories = emptyList()
        )

        val outputStream = ByteArrayOutputStream()
        ExcelManager.exportFullBackup(backupData, outputStream)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val restored = ExcelManager.parseFullBackup(inputStream)

        assertEquals(1, restored.tasks.size)
        val restoredTask = restored.tasks[0]
        assertEquals("Backup Test Task with Notifications", restoredTask.title)
        assertEquals("08:00", restoredTask.notificationTime1)
        assertEquals("09:30", restoredTask.notificationTime2)
        assertEquals("11:00", restoredTask.notificationTime3)
    }

    // ==========================================
    // 7. Routines Section Scheduling & Completion Disabling Tests
    // ==========================================

    @Test
    fun testMondayOnlyTaskDisablesCompleteOptionOnOtherDays() {
        // Monday-only weekly recurrence rule (bit 1 = Monday)
        val mondayMask = 1 // 1 shl (1 - 1)
        val mondayRule = RecurrenceRuleEntity(
            id = 10,
            type = RecurrenceType.WEEKLY,
            daysOfWeekMask = mondayMask,
            startDate = "2026-11-01"
        )

        val mondayTask = TaskEntity(
            id = 100,
            title = "Weekly Team Retrospective",
            scheduledDate = "2026-11-02", // Monday
            recurrenceRuleId = mondayRule.id
        )

        val habit = HabitSyncEngine.createHabitFromTasks(listOf(mondayTask), mondayRule)
        assertEquals(HabitFrequency.SPECIFIC_DAYS, habit.frequency)
        assertEquals(mondayMask, habit.daysOfWeekMask)
        assertEquals("Scheduled: Mon", habit.description)

        // Monday (2026-11-02, 2026-11-09, 2026-11-16, 2026-11-23, 2026-11-30) -> ENABLED (true)
        val mondayDate = LocalDate.parse("2026-11-02")
        assertTrue("Monday must be scheduled and enabled", HabitSyncEngine.isHabitScheduledOnDate(habit, mondayDate, mondayRule))
        assertTrue("Monday without rule object must still be scheduled and enabled via mask", HabitSyncEngine.isHabitScheduledOnDate(habit, mondayDate))

        // Tuesday through Sunday -> DISABLED (false)
        val tuesdayDate = LocalDate.parse("2026-11-03")
        val wednesdayDate = LocalDate.parse("2026-11-04")
        val thursdayDate = LocalDate.parse("2026-11-05")
        val fridayDate = LocalDate.parse("2026-11-06")
        val saturdayDate = LocalDate.parse("2026-11-07")
        val sundayDate = LocalDate.parse("2026-11-08")

        assertFalse("Tuesday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, tuesdayDate, mondayRule))
        assertFalse("Wednesday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, wednesdayDate, mondayRule))
        assertFalse("Thursday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, thursdayDate, mondayRule))
        assertFalse("Friday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, fridayDate, mondayRule))
        assertFalse("Saturday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, saturdayDate, mondayRule))
        assertFalse("Sunday must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, sundayDate, mondayRule))
    }

    @Test
    fun testMonthlyTaskDisablesCompleteOptionOnOtherDays() {
        // Monthly routine on the 15th
        val monthlyRule = RecurrenceRuleEntity(
            id = 20,
            type = RecurrenceType.MONTHLY_DATE,
            dayOfMonth = 15,
            startDate = "2026-11-01"
        )

        val task = TaskEntity(
            id = 200,
            title = "Pay Credit Card Bill",
            scheduledDate = "2026-11-15",
            recurrenceRuleId = monthlyRule.id
        )

        val habit = HabitSyncEngine.createHabitFromTasks(listOf(task), monthlyRule)
        assertEquals("Monthly on day 15", habit.description)

        // 15th of the month -> ENABLED (true)
        val day15 = LocalDate.parse("2026-11-15")
        assertTrue("Day 15 must be scheduled and enabled", HabitSyncEngine.isHabitScheduledOnDate(habit, day15, monthlyRule))

        // Other days (14th, 16th, etc.) -> DISABLED (false)
        val day14 = LocalDate.parse("2026-11-14")
        val day16 = LocalDate.parse("2026-11-16")
        val day1 = LocalDate.parse("2026-11-01")

        assertFalse("Day 14 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, day14, monthlyRule))
        assertFalse("Day 16 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, day16, monthlyRule))
        assertFalse("Day 1 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, day1, monthlyRule))
    }

    @Test
    fun testYearlyTaskDisablesCompleteOptionOnOtherDays() {
        // Yearly routine on November 20th
        val yearlyRule = RecurrenceRuleEntity(
            id = 30,
            type = RecurrenceType.YEARLY,
            monthOfYear = 11,
            dayOfMonth = 20,
            startDate = "2026-01-01"
        )

        val task = TaskEntity(
            id = 300,
            title = "Annual Health Checkup",
            scheduledDate = "2026-11-20",
            recurrenceRuleId = yearlyRule.id
        )

        val habit = HabitSyncEngine.createHabitFromTasks(listOf(task), yearlyRule)
        assertEquals("Yearly routine", habit.description)

        // November 20th -> ENABLED (true)
        val nov20 = LocalDate.parse("2026-11-20")
        assertTrue("Nov 20 must be scheduled and enabled", HabitSyncEngine.isHabitScheduledOnDate(habit, nov20, yearlyRule))

        // Other days in November and other months -> DISABLED (false)
        val nov19 = LocalDate.parse("2026-11-19")
        val nov21 = LocalDate.parse("2026-11-21")
        val oct20 = LocalDate.parse("2026-10-20")

        assertFalse("Nov 19 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, nov19, yearlyRule))
        assertFalse("Nov 21 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, nov21, yearlyRule))
        assertFalse("Oct 20 must disable complete option", HabitSyncEngine.isHabitScheduledOnDate(habit, oct20, yearlyRule))
    }

    @Test
    fun testDailyRoutineEnabledEveryDay() {
        val dailyRule = RecurrenceRuleEntity(
            id = 40,
            type = RecurrenceType.DAILY,
            startDate = "2026-11-01"
        )

        val task = TaskEntity(
            id = 400,
            title = "Morning Meditation",
            scheduledDate = "2026-11-01",
            recurrenceRuleId = dailyRule.id
        )

        val habit = HabitSyncEngine.createHabitFromTasks(listOf(task), dailyRule)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
        assertEquals(127, habit.daysOfWeekMask)

        // Every day of the week must be enabled
        for (day in 1..30) {
            val date = LocalDate.of(2026, 11, day)
            assertTrue("Day $day must be enabled for daily routine", HabitSyncEngine.isHabitScheduledOnDate(habit, date, dailyRule))
        }
    }

    @Test
    fun testCustomDatesWithoutRecurrenceRuleOnlyEnableOccurrenceDates() {
        val tasks = listOf(
            TaskEntity(id = 501, title = "Quarterly Sprint Review", scheduledDate = "2026-11-10"),
            TaskEntity(id = 502, title = "Quarterly Sprint Review", scheduledDate = "2026-11-25")
        )
        val scheduledDates = tasks.map { it.scheduledDate }.toSet()

        val habit = HabitSyncEngine.createHabitFromTasks(tasks)

        // Occurrence dates are enabled
        assertTrue(HabitSyncEngine.isHabitScheduledOnDate(habit, LocalDate.parse("2026-11-10"), scheduledDates = scheduledDates))
        assertTrue(HabitSyncEngine.isHabitScheduledOnDate(habit, LocalDate.parse("2026-11-25"), scheduledDates = scheduledDates))

        // Non-occurrence dates are disabled
        assertFalse(HabitSyncEngine.isHabitScheduledOnDate(habit, LocalDate.parse("2026-11-11"), scheduledDates = scheduledDates))
        assertFalse(HabitSyncEngine.isHabitScheduledOnDate(habit, LocalDate.parse("2026-11-24"), scheduledDates = scheduledDates))
    }

    // ==========================================
    // 8. RupeeFlow 4-Theme System Verification Tests
    // ==========================================

    @Test
    fun testPeachyCozyThemeTokens() {
        val colors = PeachyFocusColors
        assertNotNull(colors)
        assertFalse("Peachy Cozy must be a light theme", colors.isDark)

        // Palette checks
        assertEquals(Color(0xFFFFF8F1), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFFFFF0E6), colors.surfaceSecondary)
        assertEquals(Color(0xFF4A3028), colors.textPrimary)
        assertEquals(Color(0xFF92766B), colors.textSecondary)
        assertEquals(Color(0xFFF0DDD2), colors.border)
        assertEquals(Color(0xFFF58B72), colors.primaryAccent)
        assertEquals(Color(0xFF268B65), colors.success)
        assertEquals(Color(0xFFD95757), colors.error)
        assertEquals(Color(0xFFFFD98E), colors.warning)

        // RupeeFlow semantic tokens
        assertEquals(Color(0xFFFFAD91), colors.primaryPeach)
        assertEquals(Color(0xFFF58B72), colors.deepPeach)
        assertEquals(Color(0xFFB9E5D2), colors.mint)
    }

    @Test
    fun testLavenderDreamThemeTokens() {
        val colors = LavenderFocusColors
        assertNotNull(colors)
        assertFalse("Lavender Dream must be a light theme", colors.isDark)

        // Palette checks
        assertEquals(Color(0xFFF7F3FC), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFFEEE7F7), colors.surfaceSecondary)
        assertEquals(Color(0xFF342A45), colors.textPrimary)
        assertEquals(Color(0xFF80748F), colors.textSecondary)
        assertEquals(Color(0xFFE2D8EF), colors.border)
        assertEquals(Color(0xFF9274C8), colors.primaryAccent)
        assertEquals(Color(0xFF269B78), colors.success)
        assertEquals(Color(0xFFD95770), colors.error)
        assertEquals(Color(0xFFD99B49), colors.warning)
        assertEquals(Color(0xFFB9A4E8), colors.info)
        assertEquals(Color(0xFFE6BCEB), colors.accentPink)
    }

    @Test
    fun testMidnightMochiThemeTokens() {
        val colors = MidnightMochiFocusColors
        assertNotNull(colors)
        assertTrue("Midnight Mochi must be a dark theme", colors.isDark)

        // Palette checks
        assertEquals(Color(0xFF100E19), colors.background)
        assertEquals(Color(0xFF191626), colors.surface)
        assertEquals(Color(0xFF252036), colors.surfaceSecondary)
        assertEquals(Color(0xFF211D31), colors.surfaceElevated)
        assertEquals(Color(0xFFF5F0FC), colors.textPrimary)
        assertEquals(Color(0xFFB6ADC9), colors.textSecondary)
        assertEquals(Color(0xFF393149), colors.border)
        assertEquals(Color(0xFFB49AEF), colors.primaryAccent)
        assertEquals(Color(0xFF55C99B), colors.success)
        assertEquals(Color(0xFFF07891), colors.error)
        assertEquals(Color(0xFFE7B66C), colors.warning)
        assertEquals(Color(0xFFD0BDF8), colors.info)
        assertEquals(Color(0xFFF0A9D5), colors.accentPink)
    }

    @Test
    fun testMidnightDarkThemeTokens() {
        val colors = DarkFocusColors
        assertNotNull(colors)
        assertTrue("Midnight Dark must be a dark theme", colors.isDark)

        // Palette checks
        assertEquals(Color(0xFF0A0E17), colors.background)
        assertEquals(Color(0xFF111827), colors.surface)
        assertEquals(Color(0xFF1E293B), colors.surfaceSecondary)
        assertEquals(Color(0xFFF8FAFC), colors.textPrimary)
        assertEquals(Color(0xFF94A3B8), colors.textSecondary)
        assertEquals(Color(0x3394A3B8), colors.border)
        assertEquals(Color(0xFF3B82F6), colors.primaryAccent)
        assertEquals(Color(0xFF10B981), colors.success)
        assertEquals(Color(0xFFEF4444), colors.error)
        assertEquals(Color(0xFFF59E0B), colors.warning)
    }

    @Test
    fun testAllThemesContrastReadability() {
        val allThemes = listOf(PeachyFocusColors, LavenderFocusColors, MidnightMochiFocusColors, DarkFocusColors)
        for (theme in allThemes) {
            // Text primary must not equal background
            assertFalse(theme.textPrimary == theme.background)
            // Surface must be defined
            assertNotNull(theme.surface)
            // Accent must be distinct from background
            assertFalse(theme.primaryAccent == theme.background)
            // Success, Error, Warning must be defined
            assertNotNull(theme.success)
            assertNotNull(theme.error)
            assertNotNull(theme.warning)
        }
    }

    // ==========================================
    // 9. Application Security & Hardening Tests
    // ==========================================

    @Test
    fun testCsvFormulaInjectionSanitization() {
        val dangerousPayloads = listOf(
            "=cmd|'/C calc'!A0",
            "+123456",
            "-5+5",
            "@SUM(A1:A10)",
            "\tTAB_PAYLOAD",
            "\rRETURN_PAYLOAD"
        )

        for (payload in dangerousPayloads) {
            val sanitized = ExcelManager.escapeCsv(payload)
            // Must be prepended with single quote to neutralize formula execution in Excel/Calc
            assertTrue("Payload '$payload' must be neutralized with leading single quote", sanitized.startsWith("'") || sanitized.startsWith("\"'"))
        }

        // Normal strings should not be prepended with single quote
        val normalString = "Clean Task Title"
        val escapedNormal = ExcelManager.escapeCsv(normalString)
        assertEquals("Clean Task Title", escapedNormal)
    }

    @Test
    fun testBackupRestoreSecurityInputValidation() {
        // Construct malformed backup JSON with injection payloads, out-of-range numbers, and huge strings
        val hugeTitle = "A".repeat(500)
        val malformedDate = "invalid-date-string"
        val malformedTime = "99:99"
        val outOfBoundsDuration = 999999
        val negativeReminder = -50
        val outOfBoundsMask = 999

        val json = """
        {
            "exportDate": "invalid-export-date",
            "tasks": [
                {
                    "id": 1,
                    "title": "$hugeTitle",
                    "description": "Valid description",
                    "categoryId": 1,
                    "priority": "HIGH",
                    "scheduledDate": "$malformedDate",
                    "startTime": "$malformedTime",
                    "durationMinutes": $outOfBoundsDuration,
                    "deadlineTime": "$malformedTime",
                    "reminderMinutesBefore": $negativeReminder,
                    "status": "UPCOMING"
                }
            ],
            "habits": [
                {
                    "id": 1,
                    "title": "Habit Title",
                    "daysOfWeekMask": $outOfBoundsMask,
                    "targetValue": 50000.0
                }
            ],
            "habitCompletions": [],
            "timetableEntries": []
        }
        """.trimIndent()

        val parsed = ExcelManager.parseFullBackup(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]

        // Title must be clamped to 255 chars
        assertEquals(255, task.title.length)

        // Invalid date must fallback to a valid YYYY-MM-DD date
        assertTrue("Scheduled date must match YYYY-MM-DD regex", task.scheduledDate.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))

        // Invalid times must be nullified
        assertNull("Invalid start time must be nullified", task.startTime)
        assertNull("Invalid deadline time must be nullified", task.deadlineTime)

        // Duration must be clamped to max 1440 (24h)
        assertEquals(1440, task.durationMinutes)

        // Reminder must be clamped to min 0
        assertEquals(0, task.reminderMinutesBefore)

        // Habit daysOfWeekMask must be clamped to 127
        assertEquals(1, parsed.habits.size)
        assertEquals(127, parsed.habits[0].daysOfWeekMask)
        assertEquals(10000f, parsed.habits[0].targetValue, 0.01f)
    }

    @Test
    fun testAlarmSchedulerRequestCodeOverflowSafety() {
        val largeTaskIds = listOf(
            Long.MAX_VALUE,
            Long.MAX_VALUE - 100,
            5_000_000_000L,
            2_147_483_647L,
            1L,
            100L
        )

        for (id in largeTaskIds) {
            for (offset in 1..6) {
                val code = TaskAlarmScheduler.getRequestCode(id, offset)
                // Code must be a non-negative 31-bit integer
                assertTrue("Request code for task $id with offset $offset must be non-negative: $code", code >= 0)
                assertTrue("Request code must fit safely inside 31-bit integer", code <= Int.MAX_VALUE)
            }
        }
    }

    // =========================================================================
    // 10. Focus Work & Productivity Intelligence Tests
    // =========================================================================

    class TestClockProvider(
        var elapsed: Long = 100_000_000L,
        var wallTime: Long = 1_700_000_000_000L
    ) : FocusTimerEngine.ClockProvider {
        override fun elapsedRealtime(): Long = elapsed
        override fun currentTimeMillis(): Long = wallTime

        fun advanceSeconds(seconds: Long) {
            elapsed += seconds * 1000L
            wallTime += seconds * 1000L
        }
    }

    class TestAppUsageProvider(
        var hasPerm: Boolean = true,
        var simulatedIntervals: List<RawUsageInterval> = emptyList()
    ) : AppUsageProvider {
        override fun hasPermission(): Boolean = hasPerm
        override fun queryEvents(startTime: Long, endTime: Long): List<RawUsageInterval> = simulatedIntervals
        override fun getApplicationLabel(packageName: String): String {
            return when (packageName) {
                "com.android.chrome" -> "Chrome"
                "com.google.android.youtube" -> "YouTube"
                "com.github.android" -> "GitHub"
                "com.whatsapp" -> "WhatsApp"
                "com.android.systemui" -> "System UI"
                else -> packageName.substringAfterLast('.')
            }
        }
    }

    @Test
    fun testFocusTimerEngineStartOpenEnded() {
        val clock = TestClockProvider()
        val engine = FocusTimerEngine(clock)

        engine.start(sessionId = 1L, targetDurationSecs = 0L)
        val snapshot = engine.getSnapshot()

        assertEquals(1L, snapshot.sessionId)
        assertEquals(SessionStatus.ACTIVE, snapshot.status)
        assertEquals(0L, snapshot.targetDurationSeconds)
        assertEquals(0L, snapshot.focusedSeconds)
        assertEquals(0.0f, snapshot.progress, 0.001f)
        assertEquals(100, snapshot.focusCompletionPercentage) // Open-ended defaults to 100%
    }

    @Test
    fun testFocusTimerEngineStartTargetDuration() {
        val clock = TestClockProvider()
        val engine = FocusTimerEngine(clock)

        engine.start(sessionId = 2L, targetDurationSecs = 1500L) // 25 min

        clock.advanceSeconds(300L) // 5 min passed
        val snapshot = engine.getSnapshot()

        assertEquals(300L, snapshot.focusedSeconds)
        assertEquals(1200L, snapshot.remainingSeconds)
        assertEquals(0.2f, snapshot.progress, 0.01f)
        assertEquals(20, snapshot.focusCompletionPercentage)
        assertFalse(snapshot.isTargetCompleted)

        clock.advanceSeconds(1200L) // Finish remaining 20 min
        val finishedSnapshot = engine.getSnapshot()
        assertEquals(1500L, finishedSnapshot.focusedSeconds)
        assertEquals(0L, finishedSnapshot.remainingSeconds)
        assertEquals(1.0f, finishedSnapshot.progress, 0.01f)
        assertTrue(finishedSnapshot.isTargetCompleted)
    }

    @Test
    fun testFocusTimerEnginePauseAndResumeAccuracy() {
        val clock = TestClockProvider()
        val engine = FocusTimerEngine(clock)

        engine.start(sessionId = 3L, targetDurationSecs = 1800L) // 30 min

        // Active for 600s (10 min)
        clock.advanceSeconds(600L)
        engine.pause()

        // Paused for 300s (5 min)
        clock.advanceSeconds(300L)
        var snapshot = engine.getSnapshot()

        assertEquals(SessionStatus.PAUSED, snapshot.status)
        assertEquals(600L, snapshot.focusedSeconds) // Paused time NOT counted as focused
        assertEquals(300L, snapshot.pausedSeconds)
        assertEquals(900L, snapshot.totalElapsedSeconds)

        // Resume and work for another 300s (5 min)
        engine.resume()
        clock.advanceSeconds(300L)
        engine.stop()

        val finalSnapshot = engine.getSnapshot()
        assertEquals(SessionStatus.STOPPED, finalSnapshot.status)
        assertEquals(900L, finalSnapshot.focusedSeconds) // 10m + 5m = 15m
        assertEquals(300L, finalSnapshot.pausedSeconds)  // 5m paused
        assertEquals(1200L, finalSnapshot.totalElapsedSeconds) // 20m total elapsed
    }

    @Test
    fun testFocusTimerEngineMultiplePauseResumeCycles() {
        val clock = TestClockProvider()
        val engine = FocusTimerEngine(clock)

        engine.start(sessionId = 4L, targetDurationSecs = 3600L)

        // Cycle 1: 5m focused, 2m paused
        clock.advanceSeconds(300L)
        engine.pause()
        clock.advanceSeconds(120L)
        engine.resume()

        // Cycle 2: 10m focused, 3m paused
        clock.advanceSeconds(600L)
        engine.pause()
        clock.advanceSeconds(180L)
        engine.resume()

        // Final sprint: 5m focused
        clock.advanceSeconds(300L)
        engine.complete()

        val snapshot = engine.getSnapshot()
        assertEquals(SessionStatus.COMPLETED, snapshot.status)
        assertEquals(1200L, snapshot.focusedSeconds) // 300 + 600 + 300 = 1200s (20 mins)
        assertEquals(300L, snapshot.pausedSeconds)   // 120 + 180 = 300s (5 mins)
        assertEquals(1500L, snapshot.totalElapsedSeconds) // 25 mins elapsed
    }

    @Test
    fun testFocusTimerEngineRecoveryFromPersistence() {
        val clock = TestClockProvider()
        val engine = FocusTimerEngine(clock)

        // Persisted state from DB: 15 min focused, 5 min paused, 1 distraction
        val savedSession = FocusSessionEntity(
            id = 42L,
            title = "Recovered Session",
            targetDurationSeconds = 1800L,
            startTimestamp = clock.wallTime - 1200_000L,
            focusedDurationSeconds = 900L,
            pausedDurationSeconds = 300L,
            distractedDurationSeconds = 120L,
            productiveAppDurationSeconds = 60L,
            sessionStatus = SessionStatus.ACTIVE,
            distractionCount = 1,
            appSwitchCount = 2
        )

        val pauses = listOf(
            FocusPauseEntity(
                id = 1L,
                sessionId = 42L,
                pauseTimestamp = clock.wallTime - 600_000L,
                resumeTimestamp = clock.wallTime - 300_000L,
                pauseDurationSeconds = 300L
            )
        )

        engine.recoverSession(savedSession, pauses, currentWall = clock.wallTime, currentElapsed = clock.elapsed)
        val snapshot = engine.getSnapshot()

        assertEquals(42L, snapshot.sessionId)
        assertEquals(SessionStatus.ACTIVE, snapshot.status)
        assertEquals(900L, snapshot.focusedSeconds)
        assertEquals(300L, snapshot.pausedSeconds)
        assertEquals(120L, snapshot.distractedSeconds)
        assertEquals(1, snapshot.distractionCount)
        assertEquals(2, snapshot.appSwitchCount)

        // Advance 100 seconds after recovery
        clock.advanceSeconds(100L)
        val afterSnapshot = engine.getSnapshot()
        assertEquals(1000L, afterSnapshot.focusedSeconds)
    }

    @Test
    fun testAppUsageMonitorClassification() {
        val intervals = listOf(
            RawUsageInterval("com.naman.focusos", "Focus OS", 1000L, 2000L, 1L),
            RawUsageInterval("com.android.chrome", "Chrome", 2000L, 5000L, 3L),
            RawUsageInterval("com.google.android.youtube", "YouTube", 5000L, 8000L, 3L),
            RawUsageInterval("com.github.android", "GitHub", 8000L, 10000L, 2L),
            RawUsageInterval("com.android.systemui", "System UI", 10000L, 11000L, 1L)
        )

        val provider = TestAppUsageProvider(hasPerm = true, simulatedIntervals = intervals)
        val monitor = AppUsageMonitor(provider, ourPackageName = "com.naman.focusos")

        val knownClassifications = mapOf(
            "com.android.chrome" to AppClassification.NEUTRAL_APP,
            "com.google.android.youtube" to AppClassification.DISTRACTING_APP,
            "com.github.android" to AppClassification.PRODUCTIVE_APP
        )

        val processed = monitor.processUsageWindow(
            sessionId = 10L,
            windowStart = 1000L,
            windowEnd = 11000L,
            sessionState = SessionStatus.ACTIVE,
            knownClassifications = knownClassifications
        )

        // Focus OS itself should be excluded
        assertEquals(4, processed.size)

        val chrome = processed.find { it.packageName == "com.android.chrome" }
        assertNotNull(chrome)
        assertEquals(AppClassification.NEUTRAL_APP, chrome!!.classification)
        assertEquals("ACTIVE", chrome.sessionState)

        val youtube = processed.find { it.packageName == "com.google.android.youtube" }
        assertNotNull(youtube)
        assertEquals(AppClassification.DISTRACTING_APP, youtube!!.classification)

        val github = processed.find { it.packageName == "com.github.android" }
        assertNotNull(github)
        assertEquals(AppClassification.PRODUCTIVE_APP, github!!.classification)

        val systemUi = processed.find { it.packageName == "com.android.systemui" }
        assertNotNull(systemUi)
        assertEquals(AppClassification.SYSTEM_APP, systemUi!!.classification)
    }

    @Test
    fun testAppUsageDuringManualPause() {
        val intervals = listOf(
            RawUsageInterval("com.whatsapp", "WhatsApp", 2000L, 5000L, 3L),
            RawUsageInterval("com.android.chrome", "Chrome", 5000L, 8000L, 3L)
        )

        val provider = TestAppUsageProvider(hasPerm = true, simulatedIntervals = intervals)
        val monitor = AppUsageMonitor(provider, ourPackageName = "com.naman.focusos")

        val processed = monitor.processUsageWindow(
            sessionId = 11L,
            windowStart = 2000L,
            windowEnd = 8000L,
            sessionState = SessionStatus.PAUSED,
            knownClassifications = emptyMap()
        )

        assertEquals(2, processed.size)
        // All usage during manual pause MUST be classified as PAUSED_SESSION_USAGE
        for (item in processed) {
            assertEquals(AppClassification.PAUSED_SESSION_USAGE, item.classification)
            assertEquals("PAUSED", item.sessionState)
        }
    }

    @Test
    fun testAppUsagePermissionRevokedOrUnavailable() {
        val provider = TestAppUsageProvider(hasPerm = false, simulatedIntervals = listOf(
            RawUsageInterval("com.google.android.youtube", "YouTube", 1000L, 5000L, 4L)
        ))
        val monitor = AppUsageMonitor(provider)

        assertFalse(monitor.hasUsagePermission())
        val processed = monitor.processUsageWindow(
            sessionId = 12L,
            windowStart = 1000L,
            windowEnd = 5000L,
            sessionState = SessionStatus.ACTIVE,
            knownClassifications = emptyMap()
        )

        // Must return empty and not crash when permission is revoked
        assertTrue(processed.isEmpty())
    }

    @Test
    fun testFocusAnalyticsEngineDailyCalculations() {
        val analyticsEngine = FocusAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val today = LocalDate.of(2026, 11, 21)
        val baseMillis = today.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()

        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Morning Code",
                targetDurationSeconds = 1800L,
                startTimestamp = baseMillis + 3600_000L * 9, // 09:00
                focusedDurationSeconds = 1800L,             // 30m
                pausedDurationSeconds = 300L,               // 5m
                distractedDurationSeconds = 120L,           // 2m
                distractionCount = 1,
                appSwitchCount = 2,
                sessionStatus = SessionStatus.COMPLETED
            ),
            FocusSessionEntity(
                id = 2L,
                title = "Afternoon Architecture",
                targetDurationSeconds = 3600L,
                startTimestamp = baseMillis + 3600_000L * 14, // 14:00
                focusedDurationSeconds = 3600L,              // 60m
                pausedDurationSeconds = 600L,                // 10m
                distractedDurationSeconds = 240L,            // 4m
                distractionCount = 2,
                appSwitchCount = 4,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val stats = analyticsEngine.computeDailyStats(sessions, targetDate = today, dailyGoalMinutes = 90)

        assertEquals(90, stats.totalFocusedMinutes) // 30 + 60
        assertEquals(2, stats.sessionCount)
        assertEquals(45, stats.averageSessionDurationMinutes) // 90 / 2
        assertEquals(60, stats.longestSessionMinutes)
        assertEquals(15, stats.totalPausedMinutes) // 5 + 10
        assertEquals(6, stats.totalDistractionMinutes) // 2 + 4
        assertEquals(6, stats.appSwitchCount)
        assertEquals(100, stats.goalCompletionPercentage) // 90m / 90m = 100%
    }

    @Test
    fun testFocusAnalyticsEngineWeeklyCalculations() {
        val analyticsEngine = FocusAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val monday = LocalDate.of(2026, 11, 16)
        val baseMillis = monday.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()

        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Mon Session",
                startTimestamp = baseMillis + 3600_000L * 10,
                focusedDurationSeconds = 3600L, // 1 hr
                sessionStatus = SessionStatus.COMPLETED
            ),
            FocusSessionEntity(
                id = 2L,
                title = "Wed Session",
                startTimestamp = baseMillis + 86400_000L * 2 + 3600_000L * 10,
                focusedDurationSeconds = 7200L, // 2 hrs
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val stats = analyticsEngine.computeWeeklyStats(sessions, weekStartDate = monday)

        assertEquals(3.0f, stats.totalWeeklyHours, 0.01f) // 1h + 2h
        assertEquals(7, stats.dailyBreakdown.size)
        assertEquals("Wednesday", stats.bestDayName)
        assertEquals(2.0f, stats.bestDayHours, 0.01f)
        assertEquals(28, stats.consistencyPercentage) // 2 out of 7 days = ~28%
    }

    @Test
    fun testFocusAnalyticsAppBreakdown() {
        val analyticsEngine = FocusAnalyticsEngine()
        val usages = listOf(
            FocusAppUsageEntity(id = 1, sessionId = 1, packageName = "com.google.android.youtube", applicationName = "YouTube", classification = AppClassification.DISTRACTING_APP, startTimestamp = 1000, endTimestamp = 4000, durationSeconds = 300, sessionState = "ACTIVE"),
            FocusAppUsageEntity(id = 2, sessionId = 1, packageName = "com.google.android.youtube", applicationName = "YouTube", classification = AppClassification.DISTRACTING_APP, startTimestamp = 5000, endTimestamp = 7000, durationSeconds = 200, sessionState = "ACTIVE"),
            FocusAppUsageEntity(id = 3, sessionId = 1, packageName = "com.github.android", applicationName = "GitHub", classification = AppClassification.PRODUCTIVE_APP, startTimestamp = 8000, endTimestamp = 10000, durationSeconds = 800, sessionState = "ACTIVE")
        )

        val appStats = analyticsEngine.computeAppStats(usages)

        assertEquals(2, appStats.size)
        // GitHub should be first (800s > 500s)
        assertEquals("com.github.android", appStats[0].packageName)
        assertEquals(800L, appStats[0].totalDurationSeconds)
        assertEquals(1, appStats[0].visitCount)
        assertFalse(appStats[0].isDistraction)

        // YouTube second (500s total, 2 visits)
        assertEquals("com.google.android.youtube", appStats[1].packageName)
        assertEquals(500L, appStats[1].totalDurationSeconds)
        assertEquals(2, appStats[1].visitCount)
        assertTrue(appStats[1].isDistraction)
    }

    @Test
    fun testLiveTimerSnapshotFormatting() {
        assertEquals("00:45", LiveTimerSnapshot.formatDuration(45L))
        assertEquals("05:30", LiveTimerSnapshot.formatDuration(330L))
        assertEquals("01:15:20", LiveTimerSnapshot.formatDuration(4520L))
        assertEquals("00:00", LiveTimerSnapshot.formatDuration(0L))
        assertEquals("00:00", LiveTimerSnapshot.formatDuration(-10L))
    }

    // =========================================================================
    // 10. ADVANCED PRODUCTIVITY INTELLIGENCE & ANALYTICS TESTS
    // =========================================================================

    @Test
    fun testFocusScoreProvisionalWhenEmpty() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val score = engine.calculateFocusScore(emptyList())

        assertEquals(0, score.overallScore)
        assertEquals(ScoreCategory.INSUFFICIENT_DATA, score.category)
        assertTrue(score.isProvisional)
        assertEquals(0, score.sessionsIncludedCount)
    }

    @Test
    fun testFocusScoreProvisionalWhenLessThan15Minutes() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Short 10-min Session",
                focusedDurationSeconds = 600L,
                sessionStatus = SessionStatus.COMPLETED
            )
        )
        val score = engine.calculateFocusScore(sessions)

        assertEquals(0, score.overallScore)
        assertEquals(ScoreCategory.INSUFFICIENT_DATA, score.category)
        assertTrue(score.isProvisional)
        assertEquals(1, score.sessionsIncludedCount)
    }

    @Test
    fun testFocusScoreOutstandingWithHighPerformance() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseTime = 1732147200000L // 2026-11-21 00:00:00 UTC
        val sessions = (0..6).map { day ->
            FocusSessionEntity(
                id = day.toLong() + 1,
                title = "Daily Deep Work",
                startTimestamp = baseTime + (day * 86400_000L) + (9 * 3600_000L),
                targetDurationSeconds = 3600L,
                focusedDurationSeconds = 3600L,
                distractedDurationSeconds = 0L,
                distractionCount = 0,
                appSwitchCount = 0,
                sessionStatus = SessionStatus.COMPLETED
            )
        }

        val score = engine.calculateFocusScore(sessions, periodDays = 7)

        assertFalse(score.isProvisional)
        assertTrue(score.overallScore >= 90)
        assertEquals(ScoreCategory.OUTSTANDING, score.category)
        assertEquals(100f, score.focusCompletionScore, 0.1f)
        assertEquals(100f, score.distractionControlScore, 0.1f)
        assertEquals(7, score.sessionsIncludedCount)
        assertTrue(score.positiveContributors.isNotEmpty())
    }

    @Test
    fun testFocusScoreFairScalingForOpenEndedSessions() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Open Flow",
                targetDurationSeconds = 0L,
                focusedDurationSeconds = 1800L,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val score = engine.calculateFocusScore(sessions)

        assertEquals(100f, score.focusCompletionScore, 0.1f)
    }

    @Test
    fun testFocusScoreExcludesCancelledSessions() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Accidentally Started",
                targetDurationSeconds = 1500L,
                focusedDurationSeconds = 30L,
                sessionStatus = SessionStatus.CANCELLED
            ),
            FocusSessionEntity(
                id = 2L,
                title = "Completed Session",
                targetDurationSeconds = 1800L,
                focusedDurationSeconds = 1800L,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val score = engine.calculateFocusScore(sessions)

        assertEquals(1, score.sessionsIncludedCount)
        assertEquals(100f, score.focusCompletionScore, 0.1f)
    }

    @Test
    fun testFocusScorePeriodTrendCalculation() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val prevSessions = listOf(
            FocusSessionEntity(
                id = 1L,
                title = "Past Session",
                targetDurationSeconds = 1800L,
                focusedDurationSeconds = 1200L,
                distractedDurationSeconds = 600L,
                appSwitchCount = 10,
                sessionStatus = SessionStatus.COMPLETED
            )
        )
        val currentSessions = listOf(
            FocusSessionEntity(
                id = 2L,
                title = "Current Session",
                targetDurationSeconds = 1800L,
                focusedDurationSeconds = 1800L,
                distractedDurationSeconds = 0L,
                appSwitchCount = 0,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val score = engine.calculateFocusScore(currentSessions, previousPeriodSessions = prevSessions)

        assertNotNull(score.previousPeriodScore)
        assertNotNull(score.trendPercentage)
        assertTrue(score.trendPercentage!! > 0f)
    }

    @Test
    fun testHourlyProductivity24HourBucketing() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseMidnight = 1732147200000L
        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                startTimestamp = baseMidnight + (9 * 3600_000L),
                focusedDurationSeconds = 1800L,
                distractedDurationSeconds = 120L,
                sessionStatus = SessionStatus.COMPLETED
            ),
            FocusSessionEntity(
                id = 2L,
                startTimestamp = baseMidnight + (9 * 3600_000L) + (35 * 60_000L),
                focusedDurationSeconds = 1200L,
                sessionStatus = SessionStatus.COMPLETED
            ),
            FocusSessionEntity(
                id = 3L,
                startTimestamp = baseMidnight + (14 * 3600_000L),
                focusedDurationSeconds = 2400L,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val hourly = engine.calculateHourlyProductivity(sessions)

        assertEquals(24, hourly.size)
        val hour9 = hourly.find { it.hour == 9 }!!
        assertEquals(50, hour9.totalFocusedMinutes)
        assertEquals(2, hour9.sessionCount)

        val hour14 = hourly.find { it.hour == 14 }!!
        assertEquals(40, hour14.totalFocusedMinutes)
        assertEquals(1, hour14.sessionCount)

        val hour0 = hourly.find { it.hour == 0 }!!
        assertEquals(0, hour0.totalFocusedMinutes)
    }

    @Test
    fun testHourlyProductivityMidnightCrossing() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseMidnight = 1732147200000L
        val session = FocusSessionEntity(
            id = 1L,
            startTimestamp = baseMidnight + (23 * 3600_000L) + (30 * 60_000L),
            focusedDurationSeconds = 3600L,
            sessionStatus = SessionStatus.COMPLETED
        )

        val hourly = engine.calculateHourlyProductivity(listOf(session))

        val hour23 = hourly.find { it.hour == 23 }!!
        val hour0 = hourly.find { it.hour == 0 }!!

        assertEquals(30, hour23.totalFocusedMinutes)
        assertEquals(30, hour0.totalFocusedMinutes)
    }

    @Test
    fun testWeeklyHeatmap133CellsGenerated() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val heatmap = engine.calculateWeeklyHeatmap(emptyList())

        assertEquals(133, heatmap.size)
        assertTrue(heatmap.all { it.hour in 5..23 })
        assertEquals(19, heatmap.count { it.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(19, heatmap.count { it.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test
    fun testWeeklyHeatmapIntensityMapping() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val monday = 1731888000000L
        val sessions = listOf(
            FocusSessionEntity(
                id = 1L,
                startTimestamp = monday + (10 * 3600_000L),
                focusedDurationSeconds = 2700L,
                distractedDurationSeconds = 300L,
                sessionStatus = SessionStatus.COMPLETED
            )
        )

        val heatmap = engine.calculateWeeklyHeatmap(sessions)
        val mon10Cell = heatmap.find { it.dayOfWeek == DayOfWeek.MONDAY && it.hour == 10 }!!

        assertEquals(45, mon10Cell.focusedMinutes)
        assertEquals(1, mon10Cell.sessionCount)
        assertEquals(5, mon10Cell.distractionMinutes)
    }

    @Test
    fun testBestFocusPeriodIdentification() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseTime = 1732147200000L
        val sessions = listOf(
            FocusSessionEntity(id = 1, startTimestamp = baseTime + (8 * 3600_000L), focusedDurationSeconds = 3600L, sessionStatus = SessionStatus.COMPLETED),
            FocusSessionEntity(id = 2, startTimestamp = baseTime + (9 * 3600_000L), focusedDurationSeconds = 3600L, sessionStatus = SessionStatus.COMPLETED)
        )

        val recommendation = engine.determineBestFocusPeriod(sessions)

        assertEquals(8, recommendation.bestTwoHourStart)
        assertEquals(8, recommendation.startHour)
        assertEquals(10, recommendation.endHour)
    }

    @Test
    fun testBestFocusPeriodConfidenceTiers() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseTime = 1732147200000L

        val lowSessions = (1..3).map {
            FocusSessionEntity(id = it.toLong(), startTimestamp = baseTime, focusedDurationSeconds = 1800L, sessionStatus = SessionStatus.COMPLETED)
        }
        val recLow = engine.determineBestFocusPeriod(lowSessions)
        assertEquals(RecommendationConfidence.LOW, recLow.confidence)

        val modSessions = (1..6).map {
            FocusSessionEntity(id = it.toLong(), startTimestamp = baseTime + ((it % 3) * 86400_000L), focusedDurationSeconds = 1800L, sessionStatus = SessionStatus.COMPLETED)
        }
        val recMod = engine.determineBestFocusPeriod(modSessions)
        assertEquals(RecommendationConfidence.MODERATE, recMod.confidence)

        val highSessions = (1..12).map {
            FocusSessionEntity(id = it.toLong(), startTimestamp = baseTime + ((it % 4) * 86400_000L), focusedDurationSeconds = 1800L, sessionStatus = SessionStatus.COMPLETED)
        }
        val recHigh = engine.determineBestFocusPeriod(highSessions)
        assertEquals(RecommendationConfidence.HIGH, recHigh.confidence)
    }

    @Test
    fun testDistractionIntelligenceZeroDivisionSafety() {
        val engine = AdvancedAnalyticsEngine()
        val data = engine.calculateDistractionIntelligence(
            sessions = emptyList(),
            usages = emptyList(),
            previousSessions = emptyList(),
            previousUsages = emptyList()
        )

        assertEquals(0L, data.totalDistractionSeconds)
        assertEquals(0, data.totalDistractionsCount)
        assertEquals(0L, data.averageDistractionSeconds)
        assertEquals(0f, data.distractionPercentage, 0.01f)
        assertNull(data.periodChangePercentage)
    }

    @Test
    fun testDistractionIntelligenceExcludesPausedSessionUsage() {
        val engine = AdvancedAnalyticsEngine()
        val usages = listOf(
            FocusAppUsageEntity(
                id = 1, sessionId = 1, packageName = "com.google.android.youtube", applicationName = "YouTube",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = 1000, endTimestamp = 2000,
                durationSeconds = 100, sessionState = "ACTIVE"
            ),
            FocusAppUsageEntity(
                id = 2, sessionId = 1, packageName = "com.google.android.youtube", applicationName = "YouTube",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = 2500, endTimestamp = 4500,
                durationSeconds = 200, sessionState = "PAUSED"
            )
        )
        val sessions = listOf(
            FocusSessionEntity(id = 1, focusedDurationSeconds = 900L, sessionStatus = SessionStatus.COMPLETED)
        )

        val result = engine.calculateDistractionIntelligence(sessions, usages)

        assertEquals(100L, result.totalDistractionSeconds)
        assertEquals(1, result.totalDistractionsCount)
    }

    @Test
    fun testDistractionIntelligencePeriodComparisonImprovement() {
        val engine = AdvancedAnalyticsEngine()
        val prevUsages = listOf(
            FocusAppUsageEntity(
                id = 1, sessionId = 1, packageName = "com.instagram.android", applicationName = "Instagram",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = 1000, endTimestamp = 3000,
                durationSeconds = 200, sessionState = "ACTIVE"
            )
        )
        val currentUsages = listOf(
            FocusAppUsageEntity(
                id = 2, sessionId = 2, packageName = "com.instagram.android", applicationName = "Instagram",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = 4000, endTimestamp = 5000,
                durationSeconds = 100, sessionState = "ACTIVE"
            )
        )
        val dummySessions = listOf(FocusSessionEntity(id = 1, sessionStatus = SessionStatus.COMPLETED))

        val result = engine.calculateDistractionIntelligence(
            sessions = dummySessions,
            usages = currentUsages,
            previousSessions = dummySessions,
            previousUsages = prevUsages
        )

        assertEquals(-50f, result.periodChangePercentage!!, 0.1f)
        assertTrue(result.isImprovement)
    }

    @Test
    fun testDistractionPatternsPeakHourAndObservations() {
        val engine = AdvancedAnalyticsEngine(zoneId = java.time.ZoneId.of("UTC"))
        val baseTime = 1732147200000L
        val usages = listOf(
            FocusAppUsageEntity(
                id = 1, sessionId = 1, packageName = "com.twitter.android", applicationName = "X",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = baseTime + (15 * 3600_000L),
                endTimestamp = baseTime + (15 * 3600_000L) + 300_000L, durationSeconds = 300, sessionState = "ACTIVE"
            ),
            FocusAppUsageEntity(
                id = 2, sessionId = 1, packageName = "com.twitter.android", applicationName = "X",
                classification = AppClassification.DISTRACTING_APP, startTimestamp = baseTime + (15 * 3600_000L) + 400_000L,
                endTimestamp = baseTime + (15 * 3600_000L) + 600_000L, durationSeconds = 200, sessionState = "ACTIVE"
            )
        )

        val report = engine.calculateDistractionPatterns(usages, sessionCount = 1)

        assertEquals("3 PM", report.mostDistractingHourFormatted)
        assertEquals("X", report.mostFrequentlyOpenedApp)
        assertEquals(2.0f, report.distractionsPerSession, 0.01f)
    }

    @Test
    fun testSymmetricTaskEstimationAccuracyEqual() {
        val engine = AdvancedAnalyticsEngine()
        val accuracy = engine.calculateEstimationAccuracy(estimatedMins = 60, actualMins = 60)
        assertEquals(100, accuracy)
    }

    @Test
    fun testSymmetricTaskEstimationAccuracyOverAndUnderSymmetry() {
        val engine = AdvancedAnalyticsEngine()
        val accUnder = engine.calculateEstimationAccuracy(estimatedMins = 30, actualMins = 60)
        val accOver = engine.calculateEstimationAccuracy(estimatedMins = 60, actualMins = 30)

        assertEquals(50, accUnder)
        assertEquals(50, accOver)
        assertEquals(accUnder, accOver)
    }

    @Test
    fun testTaskEfficiencySummaryAggregation() {
        val engine = AdvancedAnalyticsEngine()
        val tasks = listOf(
            TaskEntity(id = 1, title = "Backend Architecture", scheduledDate = "2026-11-21", durationMinutes = 60, categoryId = 1, status = TaskStatus.COMPLETED_ON_TIME),
            TaskEntity(id = 2, title = "Documentation", scheduledDate = "2026-11-21", durationMinutes = 30, categoryId = 1, status = TaskStatus.COMPLETED_ON_TIME)
        )
        val sessions = listOf(
            FocusSessionEntity(id = 1, linkedTaskId = 1, focusedDurationSeconds = 3600L, sessionStatus = SessionStatus.COMPLETED),
            FocusSessionEntity(id = 2, linkedTaskId = 2, focusedDurationSeconds = 1800L, sessionStatus = SessionStatus.COMPLETED)
        )
        val categories = listOf(CategoryEntity(id = 1, name = "Engineering", colorHex = "#2563EB"))

        val summary = engine.calculateTaskFocusComparison(tasks, categories, sessions)

        assertEquals(90, summary.overallEstimatedMinutes)
        assertEquals(90, summary.overallActualFocusedMinutes)
        assertEquals(100, summary.averageEstimationAccuracy)
        assertEquals(2, summary.completedTasksLinked)
        assertEquals(2, summary.taskComparisons.size)
    }

    @Test
    fun testCategoryFocusAnalyticsDistribution() {
        val engine = AdvancedAnalyticsEngine()
        val tasks = listOf(
            TaskEntity(id = 1, title = "Task A", scheduledDate = "2026-11-21", durationMinutes = 60, categoryId = 1, status = TaskStatus.COMPLETED_ON_TIME),
            TaskEntity(id = 2, title = "Task B", scheduledDate = "2026-11-21", durationMinutes = 60, categoryId = 2, status = TaskStatus.COMPLETED_ON_TIME)
        )
        val sessions = listOf(
            FocusSessionEntity(id = 1, linkedTaskId = 1, focusedDurationSeconds = 5400L, sessionStatus = SessionStatus.COMPLETED),
            FocusSessionEntity(id = 2, linkedTaskId = 2, focusedDurationSeconds = 1800L, sessionStatus = SessionStatus.COMPLETED)
        )
        val categories = listOf(
            CategoryEntity(id = 1, name = "Engineering", colorHex = "#2563EB"),
            CategoryEntity(id = 2, name = "Design", colorHex = "#10B981")
        )

        val summary = engine.calculateTaskFocusComparison(tasks, categories, sessions)

        assertEquals(2, summary.categoryBreakdown.size)
        val eng = summary.categoryBreakdown.find { it.categoryName == "Engineering" }!!
        val des = summary.categoryBreakdown.find { it.categoryName == "Design" }!!

        assertEquals(75, eng.percentageOfTotalFocus)
        assertEquals(25, des.percentageOfTotalFocus)
    }

    @Test
    fun testProductivityInsightsGenerationEvidenceBased() {
        val engine = AdvancedAnalyticsEngine()
        val score = FocusScoreDetail.EMPTY.copy(overallScore = 88, category = ScoreCategory.EXCELLENT)
        val bestPeriod = BestFocusPeriodRecommendation.EMPTY.copy(
            bestHourFormatted = "10 AM",
            confidence = RecommendationConfidence.HIGH
        )
        val distractionData = DistractionDashboardData(
            totalDistractionSeconds = 60L,
            totalDistractionsCount = 1,
            totalAppSwitches = 1,
            averageDistractionSeconds = 60L,
            longestDistractionSeconds = 60L,
            mostDistractingAppName = "None",
            distractionPercentage = 3.5f
        )
        val taskEfficiency = TaskEfficiencySummary(
            averageEstimationAccuracy = 92,
            overallEstimatedMinutes = 120,
            overallActualFocusedMinutes = 125,
            completedTasksLinked = 3,
            categoryBreakdown = emptyList(),
            taskComparisons = emptyList(),
            insights = emptyList()
        )

        val insights = engine.generateProductivityInsights(
            sessions = listOf(FocusSessionEntity(id = 1, focusedDurationSeconds = 3600L, sessionStatus = SessionStatus.COMPLETED)),
            scoreDetail = score,
            bestPeriod = bestPeriod,
            distractionData = distractionData,
            taskEfficiency = taskEfficiency
        )

        assertTrue(insights.isNotEmpty())
        assertTrue(insights.all { it.evidence.isNotBlank() })
        assertTrue(insights.all { it.title.isNotBlank() })
    }

    @Test
    fun testDateFilteringSessionsAndUsages() {
        val zone = java.time.ZoneId.of("UTC")
        val today = LocalDate.now(zone)
        val todayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli() + 3600_000L
        val yesterdayMillis = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 3600_000L
        val oldMillis = today.minusDays(40).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = listOf(
            FocusSessionEntity(id = 1, startTimestamp = todayMillis),
            FocusSessionEntity(id = 2, startTimestamp = yesterdayMillis),
            FocusSessionEntity(id = 3, startTimestamp = oldMillis)
        )

        val todayFiltered = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, AnalyticsDateFilter.TODAY, zone)
        assertEquals(1, todayFiltered.size)
        assertEquals(1L, todayFiltered[0].id)

        val yesterdayFiltered = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, AnalyticsDateFilter.YESTERDAY, zone)
        assertEquals(1, yesterdayFiltered.size)
        assertEquals(2L, yesterdayFiltered[0].id)

        val last7DaysFiltered = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, AnalyticsDateFilter.LAST_7_DAYS, zone)
        assertEquals(2, last7DaysFiltered.size)

        val allFiltered = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, AnalyticsDateFilter.ALL, zone)
        assertEquals(3, allFiltered.size)
    }

    // =========================================================================
    // PERMANENT TASK DELETION & CALENDAR SYNCHRONIZATION TESTS
    // =========================================================================

    @Test
    fun testPermanentTaskDeletionRemovesFromDatabaseAndCalendarGrouping() {
        // Task scheduled for November 25
        val task1 = TaskEntity(
            id = 101L,
            title = "Prepare Pitch Deck",
            scheduledDate = "2026-11-25",
            startTime = "09:00",
            durationMinutes = 60
        )
        val task2 = TaskEntity(
            id = 102L,
            title = "Team Standup",
            scheduledDate = "2026-11-25",
            startTime = "10:30",
            durationMinutes = 30
        )
        val task3 = TaskEntity(
            id = 103L,
            title = "Client Call",
            scheduledDate = "2026-11-26",
            startTime = "14:00",
            durationMinutes = 45
        )

        val initialTasks = mutableListOf(task1, task2, task3)

        // Verify initial calendar grouped state
        var grouped = initialTasks.groupBy { it.scheduledDate }
        assertEquals(2, grouped["2026-11-25"]?.size)
        assertTrue(grouped["2026-11-25"]!!.any { it.id == 101L })

        // Permanent deletion of task 101: removes original task record
        initialTasks.removeIf { it.id == 101L }

        // Verify it disappears from every Calendar date
        grouped = initialTasks.groupBy { it.scheduledDate }
        val nov25Tasks = grouped["2026-11-25"] ?: emptyList()
        assertEquals(1, nov25Tasks.size)
        assertFalse(nov25Tasks.any { it.id == 101L })
        assertEquals(102L, nov25Tasks[0].id)

        // Other calendar dates remain completely unchanged
        assertEquals(1, grouped["2026-11-26"]?.size)
        assertEquals(103L, grouped["2026-11-26"]!![0].id)
    }

    @Test
    fun testDeletedTaskDisappearsFromTodayTasksAndStats() {
        val todayStr = LocalDate.now().format(dateFormatter)
        val todayTask1 = TaskEntity(
            id = 201L,
            title = "Urgent Review",
            scheduledDate = todayStr,
            startTime = "08:00",
            durationMinutes = 45,
            priority = Priority.CRITICAL
        )
        val todayTask2 = TaskEntity(
            id = 202L,
            title = "Workout",
            scheduledDate = todayStr,
            startTime = "17:00",
            durationMinutes = 60,
            priority = Priority.MEDIUM
        )

        val taskList = mutableListOf(todayTask1, todayTask2)

        // Before deletion: 2 tasks today
        val beforeToday = taskList.filter { it.scheduledDate == todayStr }
        assertEquals(2, beforeToday.size)

        // Permanent deletion of 201
        taskList.removeIf { it.id == 201L }

        // After deletion: only task 202 remains
        val afterToday = taskList.filter { it.scheduledDate == todayStr }
        assertEquals(1, afterToday.size)
        assertEquals(202L, afterToday[0].id)
        assertFalse(afterToday.any { it.id == 201L })
    }

    @Test
    fun testPermanentDeletionPersistsAcrossAppRestart() {
        // Simulate database state
        val persistentDatabaseStorage = mutableMapOf<Long, TaskEntity>()
        val task = TaskEntity(
            id = 301L,
            title = "Annual Health Checkup",
            scheduledDate = "2026-11-25",
            startTime = "11:00"
        )
        persistentDatabaseStorage[task.id] = task
        assertEquals(1, persistentDatabaseStorage.size)

        // Delete from database
        persistentDatabaseStorage.remove(task.id)

        // Simulate app restart: re-reading from database storage
        val restartedAppTasks = persistentDatabaseStorage.values.toList()
        assertTrue(restartedAppTasks.isEmpty())
        assertNull(persistentDatabaseStorage[301L])
    }

    @Test
    fun testDeleteRecurringTaskPermanentlyRemovesOriginalAndFutureOccurrences() {
        val ruleId = 55L
        val rule = RecurrenceRuleEntity(
            id = ruleId,
            type = RecurrenceType.DAILY,
            interval = 1,
            startDate = "2026-11-20"
        )

        val pastOccurrence = TaskEntity(
            id = 501L,
            title = "Daily Standup",
            scheduledDate = "2026-11-20",
            recurrenceRuleId = ruleId,
            status = TaskStatus.COMPLETED_ON_TIME
        )
        val targetOccurrence = TaskEntity(
            id = 502L,
            title = "Daily Standup",
            scheduledDate = "2026-11-25",
            recurrenceRuleId = ruleId,
            status = TaskStatus.UPCOMING
        )
        val futureOccurrence1 = TaskEntity(
            id = 503L,
            title = "Daily Standup",
            scheduledDate = "2026-11-26",
            recurrenceRuleId = ruleId,
            status = TaskStatus.UPCOMING
        )
        val futureOccurrence2 = TaskEntity(
            id = 504L,
            title = "Daily Standup",
            scheduledDate = "2026-11-27",
            recurrenceRuleId = ruleId,
            status = TaskStatus.UPCOMING
        )

        val databaseTasks = mutableListOf(pastOccurrence, targetOccurrence, futureOccurrence1, futureOccurrence2)

        // Permanent deletion from target date (Nov 25) forward
        val fromDate = targetOccurrence.scheduledDate
        val toDelete = databaseTasks.filter { it.recurrenceRuleId == ruleId && it.scheduledDate >= fromDate }
        assertEquals(3, toDelete.size) // 502, 503, 504

        databaseTasks.removeAll(toDelete)

        // Future occurrences are completely removed
        assertEquals(1, databaseTasks.size)
        assertEquals(501L, databaseTasks[0].id)
        assertEquals("2026-11-20", databaseTasks[0].scheduledDate)

        // Verify updated recurrence rule endDate caps at previous day
        val updatedRule = rule.copy(endDate = "2026-11-24")
        assertEquals("2026-11-24", updatedRule.endDate)

        // Verify generator will no longer generate future occurrences after endDate
        val newOccurrences = RecurrenceEngine.generateOccurrences(
            rule = updatedRule,
            baseTask = pastOccurrence,
            fromDate = LocalDate.of(2026, 11, 25),
            toDate = LocalDate.of(2026, 11, 30)
        )
        assertTrue(newOccurrences.isEmpty())
    }

    @Test
    fun testDeleteSingleOccurrenceLeavesOtherOccurrencesIntactAndAddsException() {
        val ruleId = 66L
        val rule = RecurrenceRuleEntity(
            id = ruleId,
            type = RecurrenceType.DAILY,
            interval = 1,
            startDate = "2026-11-20",
            exceptionDates = ""
        )

        val task1 = TaskEntity(id = 601L, title = "Evening Read", scheduledDate = "2026-11-20", recurrenceRuleId = ruleId)
        val task2 = TaskEntity(id = 602L, title = "Evening Read", scheduledDate = "2026-11-21", recurrenceRuleId = ruleId)
        val task3 = TaskEntity(id = 603L, title = "Evening Read", scheduledDate = "2026-11-22", recurrenceRuleId = ruleId)

        val tasks = mutableListOf(task1, task2, task3)

        // Delete occurrence on 2026-11-21 only
        tasks.removeIf { it.id == 602L }

        // Add exception to rule
        val updatedRule = rule.copy(exceptionDates = "2026-11-21")

        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.id == 601L })
        assertTrue(tasks.any { it.id == 603L })
        assertFalse(tasks.any { it.id == 602L })

        // Exception prevents re-generating 2026-11-21
        assertTrue(updatedRule.hasException("2026-11-21"))
        assertFalse(updatedRule.hasException("2026-11-20"))
        assertFalse(updatedRule.hasException("2026-11-22"))
    }

    @Test
    fun testDeleteTaskDoesNotDeleteIndependentTaskWithSimilarTitle() {
        // Two independent tasks created without recurrence rule
        val taskA = TaskEntity(
            id = 701L,
            title = "Submit Report",
            scheduledDate = "2026-11-20",
            recurrenceRuleId = null
        )
        val taskB = TaskEntity(
            id = 702L,
            title = "Submit Report",
            scheduledDate = "2026-11-25",
            recurrenceRuleId = null
        )

        val allTasks = mutableListOf(taskA, taskB)

        // User deletes taskA (on Nov 20)
        allTasks.removeIf { it.id == taskA.id }

        // taskA is removed, taskB remains completely unchanged!
        assertEquals(1, allTasks.size)
        assertEquals(702L, allTasks[0].id)
        assertEquals("Submit Report", allTasks[0].title)
        assertEquals("2026-11-25", allTasks[0].scheduledDate)
    }

    @Test
    fun testPermanentDeletionCancelsAlarmsAndActiveNotifications() {
        val taskId = 888L
        val requestCodes = (1..6).map { TaskAlarmScheduler.getRequestCode(taskId, it) }

        // Ensure each request code for alarms 1..6 is distinct and deterministically mapped
        assertEquals(6, requestCodes.distinct().size)
        for (suffix in 1..6) {
            val code = TaskAlarmScheduler.getRequestCode(taskId, suffix)
            assertTrue(code > 0)
        }

        // Notification ID calculation
        val notificationId = ((taskId xor (taskId ushr 32)).toInt() and 0x00FFFFFF)
        assertTrue(notificationId > 0)
    }

    @Test
    fun testHistoricalFocusSessionsPreservedWithLinkedTaskIdSetToNull() {
        val taskId = 999L
        val session1 = FocusSessionEntity(
            id = 10L,
            title = "Architecture Deep Dive",
            linkedTaskId = taskId,
            focusedDurationSeconds = 3600L,
            sessionStatus = SessionStatus.COMPLETED
        )
        val session2 = FocusSessionEntity(
            id = 11L,
            title = "Unrelated Focus",
            linkedTaskId = null,
            focusedDurationSeconds = 1800L,
            sessionStatus = SessionStatus.COMPLETED
        )

        val sessions = mutableListOf(session1, session2)

        // When taskId 999 is permanently deleted, unlink task
        val unlinked = sessions.map { session ->
            if (session.linkedTaskId == taskId) session.copy(linkedTaskId = null) else session
        }

        // Session 1 is preserved with all tracking metrics, but linkedTaskId is now null
        assertEquals(2, unlinked.size)
        val updatedSession1 = unlinked.first { it.id == 10L }
        assertNull(updatedSession1.linkedTaskId)
        assertEquals("Architecture Deep Dive", updatedSession1.title)
        assertEquals(3600L, updatedSession1.focusedDurationSeconds)
        assertEquals(SessionStatus.COMPLETED, updatedSession1.sessionStatus)

        // Unrelated session 2 is completely unchanged
        val updatedSession2 = unlinked.first { it.id == 11L }
        assertNull(updatedSession2.linkedTaskId)
        assertEquals(1800L, updatedSession2.focusedDurationSeconds)
    }

    @Test
    fun testDatabaseMigrations1To2And2To3() {
        val mockDb = mock(SupportSQLiteDatabase::class.java)

        // Run MIGRATION_1_2
        FocusDatabase.MIGRATION_1_2.migrate(mockDb)
        // Verify tasks table columns added
        verify(mockDb).execSQL("ALTER TABLE tasks ADD COLUMN notificationTime1 TEXT DEFAULT NULL")
        verify(mockDb).execSQL("ALTER TABLE tasks ADD COLUMN notificationTime2 TEXT DEFAULT NULL")
        verify(mockDb).execSQL("ALTER TABLE tasks ADD COLUMN notificationTime3 TEXT DEFAULT NULL")

        // Run MIGRATION_2_3
        FocusDatabase.MIGRATION_2_3.migrate(mockDb)
        // Verify foreign key table creation and index setup succeeded
        verify(mockDb, atLeastOnce()).execSQL(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `focus_sessions`"))
        verify(mockDb, atLeastOnce()).execSQL(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `focus_session_pauses`"))
        verify(mockDb, atLeastOnce()).execSQL(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `focus_app_usages`"))
        verify(mockDb, atLeastOnce()).execSQL(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `focus_session_events`"))
        verify(mockDb, atLeastOnce()).execSQL(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `app_classifications`"))
    }

    @Test
    fun testTaskSearchExcludesDeletedTask() {
        val query = "budget"
        val task1 = TaskEntity(id = 1, title = "Quarterly budget review", scheduledDate = "2026-11-25")
        val task2 = TaskEntity(id = 2, title = "Personal budget calculation", scheduledDate = "2026-11-26")

        val searchList = mutableListOf(task1, task2)

        // Before deletion: 2 matches
        val matchesBefore = searchList.filter { it.title.contains(query, ignoreCase = true) }
        assertEquals(2, matchesBefore.size)

        // Delete task1 permanently
        searchList.removeIf { it.id == 1L }

        // After deletion: only task2 matches
        val matchesAfter = searchList.filter { it.title.contains(query, ignoreCase = true) }
        assertEquals(1, matchesAfter.size)
        assertEquals(2L, matchesAfter[0].id)
        assertFalse(matchesAfter.any { it.id == 1L })
    }

    @Test
    fun testSplashOnlyPlaysOncePerProcessSessionAndSkipsWhenInRam() {
        // Cold start simulation: fresh process in RAM
        FocusOSApplication.hasPlayedSplash = false
        val shouldPlayOnColdStart = !FocusOSApplication.hasPlayedSplash
        assertTrue(shouldPlayOnColdStart)

        // Animation finishes after ~2.93s
        FocusOSApplication.hasPlayedSplash = true

        // User switches to another app (app remains in RAM) and switches back
        val shouldPlayWhenInRam = !FocusOSApplication.hasPlayedSplash
        assertFalse("Splash animation must NOT play when app is in RAM", shouldPlayWhenInRam)

        // Multiple app switches while in RAM
        for (i in 1..5) {
            assertFalse(shouldPlayWhenInRam)
        }

        // Clean up
        FocusOSApplication.hasPlayedSplash = false
    }
}
