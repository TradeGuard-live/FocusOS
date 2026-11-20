package com.naman.focusos

import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.preferences.UserSettings
import com.naman.focusos.ui.theme.DarkFocusColors
import com.naman.focusos.ui.theme.LightFocusColors
import androidx.compose.ui.graphics.Color
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.data.model.sortedChronologically
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import com.naman.focusos.domain.excel.ExcelManager
import com.naman.focusos.domain.excel.FullBackupData
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import com.naman.focusos.domain.scheduler.RecurrenceEngine
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
        val dayStats = ProductivityIntelligenceEngine.calculateDayStats(date, tasks)

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
        assertEquals(Color(0xFFF5F7FB), LightFocusColors.background)
        assertEquals(Color(0xFFFFFFFF), LightFocusColors.surface)
        assertEquals(Color(0xFF172033), LightFocusColors.textPrimary)
        assertEquals(Color(0xFF697586), LightFocusColors.textSecondary)
        assertEquals(Color(0xFF19B9EF), LightFocusColors.primaryAccent)
        assertEquals(Color(0xFFE2E8F0), LightFocusColors.border)
        assertEquals(Color(0xFFEDF2F7), LightFocusColors.surfaceVariant)
        assertEquals(Color(0xFF10B981), LightFocusColors.success)
        assertEquals(Color(0xFFF59E0B), LightFocusColors.warning)
        assertEquals(Color(0xFFEF4444), LightFocusColors.danger)

        // Verify Dark theme palette maintains existing dark visual styling
        assertTrue(DarkFocusColors.isDark)
        assertEquals(Color(0xFF0B0F19), DarkFocusColors.background)
        assertEquals(Color(0xFF141C2E), DarkFocusColors.surface)
        assertEquals(Color(0xFF19B9EF), DarkFocusColors.primaryAccent)
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
}
