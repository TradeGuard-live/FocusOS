package com.naman.focusos.ui.screens.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.data.repository.HabitRepository
import com.naman.focusos.data.repository.TaskRepository
import com.naman.focusos.data.repository.TimetableRepository
import com.naman.focusos.domain.excel.ExcelManager
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RoutineTab(val title: String) {
    HABITS("Habit Tracker"),
    TIMETABLE("Daily Timetable")
}

data class RoutinesUiState(
    val activeTab: RoutineTab = RoutineTab.HABITS,
    val habits: List<HabitEntity> = emptyList(),
    val habitCompletions: List<HabitCompletionEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val importMessage: String? = null,
    val recurrenceRulesByHabitId: Map<Long, RecurrenceRuleEntity> = emptyMap(),
    val taskDatesByHabitId: Map<Long, Set<String>> = emptyMap()
) {
    fun isHabitScheduledOnDate(habit: HabitEntity, date: LocalDate): Boolean {
        val rule = recurrenceRulesByHabitId[habit.id]
        val dates = taskDatesByHabitId[habit.id]
        return HabitSyncEngine.isHabitScheduledOnDate(habit, date, rule, dates)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(application: Application) : AndroidViewModel(application) {

    private val habitRepository = HabitRepository(application)
    private val taskRepository = TaskRepository(application)
    private val timetableRepository = TimetableRepository(application)

    private val _activeTab = MutableStateFlow(RoutineTab.HABITS)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _importMessage = MutableStateFlow<String?>(null)

    init {
        // Automatically sync all recurring/multi-day tasks into Active Habits
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                habitRepository.syncTasksToHabits(tasks)
            }
        }
    }

    val uiState: StateFlow<RoutinesUiState> = _currentMonth.flatMapLatest { month ->
        val monthPrefix = "${month.year}-${String.format(Locale.US, "%02d", month.monthValue)}"
        combine(
            _activeTab,
            _importMessage,
            habitRepository.getActiveHabits(),
            habitRepository.getCompletionsForMonth(monthPrefix),
            timetableRepository.getAllEntries()
        ) { tab, msg, habits, completions, timetable ->
            combine(
                taskRepository.getAllTasks(),
                taskRepository.getAllRecurrenceRules()
            ) { tasks, rules ->
                val rulesById = rules.associateBy { it.id }
                val tasksByTitle = tasks.groupBy { it.title.trim().lowercase() }

                val recurrenceRulesByHabitId = mutableMapOf<Long, RecurrenceRuleEntity>()
                val taskDatesByHabitId = mutableMapOf<Long, Set<String>>()

                for (habit in habits) {
                    val habitTasks = tasksByTitle[habit.title.trim().lowercase()] ?: emptyList()
                    val dates = habitTasks.map { it.scheduledDate }.toSet()
                    taskDatesByHabitId[habit.id] = dates

                    val ruleId = habitTasks.mapNotNull { it.recurrenceRuleId }.firstOrNull()
                    if (ruleId != null) {
                        rulesById[ruleId]?.let { recurrenceRulesByHabitId[habit.id] = it }
                    }
                }

                RoutinesUiState(
                    activeTab = tab,
                    currentMonth = month,
                    importMessage = msg,
                    habits = habits,
                    habitCompletions = completions,
                    timetableEntries = timetable,
                    recurrenceRulesByHabitId = recurrenceRulesByHabitId,
                    taskDatesByHabitId = taskDatesByHabitId
                )
            }
        }.flatMapLatest { it }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutinesUiState()
    )

    fun setActiveTab(tab: RoutineTab) {
        _activeTab.value = tab
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun toggleHabitDay(habitId: Long, dateStr: String) {
        val date = try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            return
        }
        val habit = uiState.value.habits.firstOrNull { it.id == habitId } ?: return
        if (!uiState.value.isHabitScheduledOnDate(habit, date)) {
            // Task/Routine is not scheduled on this day (e.g. Monday-only routine on other days)
            // Complete option is strictly disabled
            return
        }
        viewModelScope.launch {
            val isNowCompleted = habitRepository.toggleHabitCompletion(habitId, dateStr)
            val habitEntity = habitRepository.getHabitById(habitId)
            if (habitEntity != null) {
                // If a task exists for this habit on this date, sync task completion state as well
                val tasks = taskRepository.getTasksForDateSync(dateStr)
                val matchingTask = tasks.firstOrNull { it.title.trim().equals(habitEntity.title.trim(), ignoreCase = true) }
                if (matchingTask != null) {
                    val updatedTask = if (isNowCompleted) {
                        matchingTask.markCompleted(LocalDateTime.now())
                    } else {
                        matchingTask.markIncomplete()
                    }
                    taskRepository.updateTask(updatedTask)
                }
            }
        }
    }

    fun createHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.insertHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun toggleTimetableEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            timetableRepository.toggleEntryEnabled(entry)
        }
    }

    fun addTimetableEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            timetableRepository.insertEntry(entry)
        }
    }

    fun deleteTimetableEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            timetableRepository.deleteEntry(entry)
        }
    }

    fun importExcelHabits(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val preview = ExcelManager.parseHabitTrackerWorkbook(inputStream)
                val count = habitRepository.importHabitsFromPreview(preview, _currentMonth.value)
                _importMessage.value = "Successfully imported ${preview.habitTitles.size} habits and $count completions!"
            } catch (e: Exception) {
                _importMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }
}
