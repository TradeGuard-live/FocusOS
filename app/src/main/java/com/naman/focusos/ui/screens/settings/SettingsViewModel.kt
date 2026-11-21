package com.naman.focusos.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.data.preferences.UserPreferencesManager
import com.naman.focusos.data.preferences.UserSettings
import com.naman.focusos.data.repository.FocusSessionRepository
import com.naman.focusos.domain.excel.ExcelManager
import com.naman.focusos.domain.excel.FullBackupData
import com.naman.focusos.domain.focus.AndroidAppUsageProvider
import com.naman.focusos.domain.focus.AppUsageMonitor
import com.naman.focusos.widget.FocusAppWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = UserPreferencesManager(application)
    private val db = FocusDatabase.getInstance(application)
    private val focusRepo = FocusSessionRepository(application)
    private val usageMonitor = AppUsageMonitor(AndroidAppUsageProvider(application))
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val settings: StateFlow<UserSettings> = prefsManager.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    val classifications: StateFlow<List<AppClassificationEntity>> = focusRepo.getAllClassifications().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun hasUsagePermission(): Boolean = usageMonitor.hasUsagePermission()

    fun setFocusMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setFocusMonitoringEnabled(enabled) }
    }

    fun setFocusNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setFocusNotificationsEnabled(enabled) }
    }

    fun setFocusDefaultDuration(minutes: Int) {
        viewModelScope.launch { prefsManager.setFocusDefaultDuration(minutes) }
    }

    fun setFocusDailyGoal(minutes: Int) {
        viewModelScope.launch { prefsManager.setFocusDailyGoal(minutes) }
    }

    fun updateClassification(
        packageName: String,
        appName: String,
        classification: AppClassification,
        isExcluded: Boolean = false
    ) {
        viewModelScope.launch {
            focusRepo.updateAppClassification(packageName, appName, classification, isExcluded)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }

    fun setTimeFormat(format: String) {
        viewModelScope.launch { prefsManager.setTimeFormat(format) }
    }

    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch { prefsManager.setFirstDayOfWeek(day) }
    }

    fun setDefaultDuration(minutes: Int) {
        viewModelScope.launch { prefsManager.setDefaultDuration(minutes) }
    }

    fun setDefaultReminder(minutes: Int) {
        viewModelScope.launch { prefsManager.setDefaultReminder(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setNotificationsEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setVibrationEnabled(enabled) }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Export Tasks to CSV stream
     */
    fun exportTasksCsv(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tasks = db.taskDao().getAllTasksSync()
                ExcelManager.exportTasksToCsv(tasks, outputStream)
                _statusMessage.value = "Exported ${tasks.size} tasks to CSV successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    /**
     * Export Habits to XLSX stream
     */
    fun exportHabitsXlsx(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val habits = db.habitDao().getAllHabitsSync()
                val completions = db.habitDao().getAllCompletionsSync()
                ExcelManager.exportHabitsToXlsx(habits, completions, YearMonth.now(), outputStream)
                _statusMessage.value = "Exported ${habits.size} habits to XLSX successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    /**
     * Full JSON Backup
     */
    fun exportFullBackup(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tasks = db.taskDao().getAllTasksSync()
                val rules = db.recurrenceRuleDao().getAllRulesSync()
                val habits = db.habitDao().getAllHabitsSync()
                val completions = db.habitDao().getAllCompletionsSync()
                val timetable = db.timetableDao().getAllEntriesSync()
                val categories = db.categoryDao().getAllCategoriesSync()

                val backup = FullBackupData(
                    exportDate = LocalDate.now().toString(),
                    tasks = tasks,
                    recurrenceRules = rules,
                    habits = habits,
                    habitCompletions = completions,
                    timetableEntries = timetable,
                    categories = categories
                )

                ExcelManager.exportFullBackup(backup, outputStream)
                _statusMessage.value = "Full backup completed successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.message}"
            }
        }
    }

    /**
     * Full JSON Restore
     */
    fun restoreFullBackup(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = ExcelManager.parseFullBackup(inputStream)
                if (data.tasks.isNotEmpty()) db.taskDao().insertTasks(data.tasks)
                if (data.habits.isNotEmpty()) db.habitDao().insertHabits(data.habits)
                if (data.habitCompletions.isNotEmpty()) db.habitDao().insertCompletions(data.habitCompletions)
                if (data.timetableEntries.isNotEmpty()) db.timetableDao().insertEntries(data.timetableEntries)

                FocusAppWidgetProvider.sendRefreshBroadcast(getApplication())
                _statusMessage.value = "Restored ${data.tasks.size} tasks and ${data.habits.size} habits!"
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.message}"
            }
        }
    }

    /**
     * Seed realistic sample data across the week (including completed on time, late, missed, upcoming)
     * and habits matching Habit_Tracker.xlsx for immediate testing!
     */
    fun seedSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now()

                // Habits matching Habit_Tracker.xlsx
                val sampleHabitNames = listOf(
                    "Wake up at 6 AM",
                    "Drink 2L Water",
                    "Workout",
                    "Healthy Breakfast",
                    "Project Work",
                    "Reading",
                    "Walk Outside",
                    "No Junk Food",
                    "Meditation",
                    "No Phone 1 Hr",
                    "Journal",
                    "Sleep by 11 PM"
                )

                val habitEntities = sampleHabitNames.mapIndexed { idx, title ->
                    HabitEntity(
                        title = title,
                        description = "Daily discipline habit",
                        colorHex = listOf("#38BDF8", "#10B981", "#8B5CF6", "#F59E0B", "#06B6D4", "#EC4899")[idx % 6]
                    )
                }
                val habitIds = db.habitDao().insertHabits(habitEntities)

                // Seed habit completions for past 10 days
                val completions = mutableListOf<HabitCompletionEntity>()
                for (hId in habitIds) {
                    for (offset in 0..15) {
                        val d = today.minusDays(offset.toLong()).format(dateFormatter)
                        if ((hId.toInt() + offset) % 3 != 0) {
                            completions.add(
                                HabitCompletionEntity(
                                    habitId = hId,
                                    date = d,
                                    isCompleted = true
                                )
                            )
                        }
                    }
                }
                db.habitDao().insertCompletions(completions)

                // Tasks for past 6 days + today + tomorrow
                val sampleTasks = listOf(
                    // Today
                    TaskEntity(title = "Morning Cardio & Stretch", categoryId = 3, priority = Priority.HIGH, scheduledDate = today.format(dateFormatter), startTime = "06:30", durationMinutes = 45, deadlineTime = "07:30", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 4 * 3600 * 1000L),
                    TaskEntity(title = "Complete DSA Practice: Graphs", categoryId = 1, priority = Priority.CRITICAL, scheduledDate = today.format(dateFormatter), startTime = "10:00", durationMinutes = 60, deadlineTime = "11:30", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 2 * 3600 * 1000L),
                    TaskEntity(title = "Review Architecture & Clean Code", categoryId = 5, priority = Priority.HIGH, scheduledDate = today.format(dateFormatter), startTime = "14:00", durationMinutes = 90, deadlineTime = "16:00", status = TaskStatus.COMPLETED_LATE, actualCompletionTime = System.currentTimeMillis() - 1 * 3600 * 1000L),
                    TaskEntity(title = "Database Migration Testing", categoryId = 5, priority = Priority.CRITICAL, scheduledDate = today.format(dateFormatter), startTime = "17:00", durationMinutes = 60, deadlineTime = "18:30", status = TaskStatus.UPCOMING),
                    TaskEntity(title = "Evening Reading & Journal", categoryId = 2, priority = Priority.MEDIUM, scheduledDate = today.format(dateFormatter), startTime = "21:30", durationMinutes = 30, deadlineTime = "22:30", status = TaskStatus.UPCOMING),

                    // Yesterday (Completed on time & missed)
                    TaskEntity(title = "System Design Lecture", categoryId = 1, priority = Priority.HIGH, scheduledDate = today.minusDays(1).format(dateFormatter), startTime = "09:00", durationMinutes = 90, deadlineTime = "11:00", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 24 * 3600 * 1000L),
                    TaskEntity(title = "Full Body Gym Session", categoryId = 3, priority = Priority.HIGH, scheduledDate = today.minusDays(1).format(dateFormatter), startTime = "17:30", durationMinutes = 60, deadlineTime = "19:00", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 22 * 3600 * 1000L),
                    TaskEntity(title = "Submit Weekly Report", categoryId = 1, priority = Priority.CRITICAL, scheduledDate = today.minusDays(1).format(dateFormatter), startTime = "20:00", durationMinutes = 30, deadlineTime = "21:00", status = TaskStatus.MISSED),

                    // 2 days ago
                    TaskEntity(title = "Algorithms Benchmark Test", categoryId = 5, priority = Priority.HIGH, scheduledDate = today.minusDays(2).format(dateFormatter), startTime = "10:30", durationMinutes = 120, deadlineTime = "13:00", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 48 * 3600 * 1000L),
                    TaskEntity(title = "Read 20 Pages Tech Book", categoryId = 2, priority = Priority.MEDIUM, scheduledDate = today.minusDays(2).format(dateFormatter), startTime = "21:00", durationMinutes = 45, deadlineTime = "22:00", status = TaskStatus.COMPLETED_LATE, actualCompletionTime = System.currentTimeMillis() - 46 * 3600 * 1000L),

                    // 3 days ago
                    TaskEntity(title = "LeetCode Weekly Contest Practice", categoryId = 1, priority = Priority.CRITICAL, scheduledDate = today.minusDays(3).format(dateFormatter), startTime = "11:00", durationMinutes = 90, deadlineTime = "13:00", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 72 * 3600 * 1000L),
                    TaskEntity(title = "Rest & Hydration Recovery", categoryId = 3, priority = Priority.LOW, scheduledDate = today.minusDays(3).format(dateFormatter), startTime = "16:00", durationMinutes = 30, deadlineTime = "17:00", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 70 * 3600 * 1000L),

                    // 4 days ago
                    TaskEntity(title = "Sprint Planning Session", categoryId = 1, priority = Priority.HIGH, scheduledDate = today.minusDays(4).format(dateFormatter), startTime = "10:00", durationMinutes = 60, deadlineTime = "11:30", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 96 * 3600 * 1000L),
                    TaskEntity(title = "Mobile UI/UX Refinement", categoryId = 5, priority = Priority.MEDIUM, scheduledDate = today.minusDays(4).format(dateFormatter), startTime = "14:00", durationMinutes = 90, deadlineTime = "16:00", status = TaskStatus.MISSED),

                    // 5 days ago
                    TaskEntity(title = "Database Index Optimization", categoryId = 5, priority = Priority.CRITICAL, scheduledDate = today.minusDays(5).format(dateFormatter), startTime = "15:00", durationMinutes = 60, deadlineTime = "16:30", status = TaskStatus.COMPLETED_ON_TIME, actualCompletionTime = System.currentTimeMillis() - 120 * 3600 * 1000L),

                    // Tomorrow
                    TaskEntity(title = "Android Jetpack Compose Deep Dive", categoryId = 5, priority = Priority.HIGH, scheduledDate = today.plusDays(1).format(dateFormatter), startTime = "10:00", durationMinutes = 90, deadlineTime = "12:00", status = TaskStatus.UPCOMING),
                    TaskEntity(title = "Leg Workout & Core", categoryId = 3, priority = Priority.HIGH, scheduledDate = today.plusDays(1).format(dateFormatter), startTime = "17:30", durationMinutes = 60, deadlineTime = "19:00", status = TaskStatus.UPCOMING)
                )

                db.taskDao().insertTasks(sampleTasks)
                FocusAppWidgetProvider.sendRefreshBroadcast(getApplication())
                _statusMessage.value = "Sample productivity & habit data loaded successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Seed failed: ${e.message}"
            }
        }
    }

    /**
     * Complete Factory Reset:
     * - Cancels all pending alarms
     * - Deletes all tasks, rules, habit records, and timetable entries
     * - Re-seeds defaults
     * - Resets all preferences
     * - Refreshes home-screen widget
     */
    fun eraseAllDataAndResetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Cancel all alarms
                val scheduler = com.naman.focusos.notification.TaskAlarmScheduler(getApplication())
                val allTasks = db.taskDao().getAllTasksSync()
                for (task in allTasks) {
                    scheduler.cancelTaskReminders(task.id)
                }

                // 2. Clear all Room tables
                db.taskDao().deleteAllTasks()
                db.recurrenceRuleDao().deleteAllRules()
                db.habitDao().deleteAllCompletions()
                db.habitDao().deleteAllHabits()
                db.timetableDao().deleteAllEntries()
                db.categoryDao().deleteAllCategories()

                // 3. Re-seed default categories and default timetable
                db.categoryDao().insertCategories(com.naman.focusos.data.model.CategoryEntity.defaultCategories())
                db.timetableDao().insertEntries(com.naman.focusos.data.model.TimetableEntryEntity.defaultTimetable())

                // 4. Reset preferences to default
                prefsManager.resetAllPreferences()

                // 5. Update home-screen widget
                FocusAppWidgetProvider.sendRefreshBroadcast(getApplication())

                _statusMessage.value = "All data erased and app reset to factory defaults!"
            } catch (e: Exception) {
                _statusMessage.value = "Reset failed: ${e.message}"
            }
        }
    }
}
