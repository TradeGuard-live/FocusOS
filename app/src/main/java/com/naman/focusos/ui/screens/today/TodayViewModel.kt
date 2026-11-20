package com.naman.focusos.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.data.model.sortedChronologically
import com.naman.focusos.data.repository.CategoryRepository
import com.naman.focusos.data.repository.TaskRepository
import com.naman.focusos.data.repository.TimetableRepository
import com.naman.focusos.domain.analytics.DayStats
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<TaskEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val activeTask: TaskEntity? = null,
    val upcomingTasks: List<TaskEntity> = emptyList(),
    val overdueTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val dayStats: DayStats? = null,
    val searchQuery: String = "",
    val isCompletedCollapsed: Boolean = false
)

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val timetableRepository = TimetableRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _searchQuery = MutableStateFlow("")
    private val _isCompletedCollapsed = MutableStateFlow(false)

    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        _searchQuery,
        _isCompletedCollapsed,
        categoryRepository.getAllCategories(),
        timetableRepository.getEnabledEntries()
    ) { date, query, collapsed, categories, timetable ->
        Tuple5(date, query, collapsed, categories, timetable)
    }.combine(
        taskRepository.getAllTasks()
    ) { tuple, allTasks ->
        val date = tuple.date
        val query = tuple.query
        val collapsed = tuple.collapsed
        val categories = tuple.categories
        val timetable = tuple.timetable

        val dateStr = date.format(dateFormatter)
        val now = LocalDateTime.now()

        // Filter tasks for selected date and sort chronologically
        val todayTasks = allTasks.filter { it.scheduledDate == dateStr }
            .filter {
                if (query.isBlank()) true
                else it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
            .sortedChronologically()

        // Evaluate active, upcoming, overdue, completed
        var active: TaskEntity? = null
        val upcoming = mutableListOf<TaskEntity>()
        val overdue = mutableListOf<TaskEntity>()
        val completed = mutableListOf<TaskEntity>()

        for (task in todayTasks) {
            val status = task.evaluateEffectiveStatus(now)
            when (status) {
                TaskStatus.IN_PROGRESS -> {
                    active = task
                    upcoming.add(task)
                }
                TaskStatus.UPCOMING -> upcoming.add(task)
                TaskStatus.MISSED -> overdue.add(task)
                TaskStatus.COMPLETED_ON_TIME, TaskStatus.COMPLETED_LATE -> completed.add(task)
                TaskStatus.SKIPPED -> completed.add(task)
            }
        }

        val stats = ProductivityIntelligenceEngine.calculateDayStats(date, todayTasks, now)

        TodayUiState(
            selectedDate = date,
            tasks = todayTasks,
            timetableEntries = timetable,
            categories = categories,
            activeTask = active,
            upcomingTasks = upcoming.sortedChronologically(),
            overdueTasks = overdue.sortedChronologically(),
            completedTasks = completed.sortedChronologically(),
            dayStats = stats,
            searchQuery = query,
            isCompletedCollapsed = collapsed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleCompletedCollapsed() {
        _isCompletedCollapsed.value = !_isCompletedCollapsed.value
    }

    fun toggleTaskComplete(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun skipTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskRepository.updateTask(task.markSkipped())
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun createTask(task: TaskEntity, recurrenceRule: RecurrenceRuleEntity?) {
        viewModelScope.launch {
            if (recurrenceRule != null) {
                taskRepository.createRecurringTask(task, recurrenceRule)
            } else {
                taskRepository.insertTask(task)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val date: A,
    val query: B,
    val collapsed: C,
    val categories: D,
    val timetable: E
)
