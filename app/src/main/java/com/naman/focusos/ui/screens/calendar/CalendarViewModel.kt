package com.naman.focusos.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.sortedChronologically
import com.naman.focusos.data.repository.CategoryRepository
import com.naman.focusos.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class CalendarViewMode(val title: String) {
    MONTH("Month"),
    WEEK("Week"),
    DAY("Day"),
    YEAR("Year")
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val tasksByDate: Map<String, List<TaskEntity>> = emptyMap(),
    val selectedDateTasks: List<TaskEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        _viewMode,
        taskRepository.getAllTasks(),
        categoryRepository.getAllCategories()
    ) { month, date, mode, tasks, categories ->
        val grouped = tasks.groupBy { it.scheduledDate }.mapValues { (_, list) ->
            list.sortedChronologically()
        }
        val dateStr = date.format(dateFormatter)
        val dateTasks = (grouped[dateStr] ?: emptyList()).sortedChronologically()

        CalendarUiState(
            currentMonth = month,
            selectedDate = date,
            viewMode = mode,
            tasksByDate = grouped,
            selectedDateTasks = dateTasks,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun toggleTaskComplete(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun createTask(task: TaskEntity, rule: RecurrenceRuleEntity?) {
        viewModelScope.launch {
            if (rule != null) {
                taskRepository.createRecurringTask(task, rule)
            } else {
                taskRepository.insertTask(task)
            }
        }
    }

    fun updateTask(task: TaskEntity, rule: RecurrenceRuleEntity? = null) {
        viewModelScope.launch {
            taskRepository.updateTaskSeries(task, rule)
        }
    }
}
