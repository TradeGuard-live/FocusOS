package com.naman.focusos.ui.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.repository.HabitRepository
import com.naman.focusos.data.repository.TaskRepository
import com.naman.focusos.domain.analytics.MonthlyAnalytics
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import com.naman.focusos.domain.analytics.WeeklyAnalytics
import com.naman.focusos.domain.analytics.YearlyAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class AnalyticsPeriod(val title: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
    val weekStartDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)),
    val currentMonth: YearMonth = YearMonth.now(),
    val currentYear: Int = LocalDate.now().year,
    val weeklyAnalytics: WeeklyAnalytics? = null,
    val monthlyAnalytics: MonthlyAnalytics? = null,
    val yearlyAnalytics: YearlyAnalytics? = null
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val habitRepository = HabitRepository(application)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.WEEKLY)
    private val _weekStartDate = MutableStateFlow(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _currentYear = MutableStateFlow(LocalDate.now().year)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        _weekStartDate,
        _currentMonth,
        _currentYear
    ) { period, weekStart, month, year ->
        PeriodParams(period, weekStart, month, year)
    }.combine(
        taskRepository.getAllTasks()
    ) { params, tasks ->
        Pair(params, tasks)
    }.combine(
        habitRepository.getActiveHabits()
    ) { (params, tasks), habits ->
        Triple(params, tasks, habits)
    }.combine(
        habitRepository.getCompletionsForMonth("${YearMonth.now().year}-${String.format("%02d", YearMonth.now().monthValue)}")
    ) { (params, tasks, habits), completions ->
        val now = LocalDateTime.now()

        // 1. Weekly calculation
        val tasksByDate = tasks.groupBy {
            try { LocalDate.parse(it.scheduledDate, dateFormatter) } catch (e: Exception) { LocalDate.MIN }
        }
        val weekly = ProductivityIntelligenceEngine.calculateWeeklyAnalytics(
            weekStartDate = params.weekStart,
            tasksByDate = tasksByDate,
            now = now
        )

        // 2. Monthly calculation
        val monthlyTasks = tasks.filter {
            try {
                val d = LocalDate.parse(it.scheduledDate, dateFormatter)
                d.year == params.month.year && d.monthValue == params.month.monthValue
            } catch (e: Exception) { false }
        }
        val monthly = ProductivityIntelligenceEngine.calculateMonthlyAnalytics(
            yearMonth = params.month,
            tasks = monthlyTasks,
            habits = habits,
            completions = completions,
            now = now
        )

        // 3. Yearly calculation
        val yearlyTasks = tasks.filter {
            try {
                val d = LocalDate.parse(it.scheduledDate, dateFormatter)
                d.year == params.year
            } catch (e: Exception) { false }
        }
        val yearly = ProductivityIntelligenceEngine.calculateYearlyAnalytics(
            year = params.year,
            tasks = yearlyTasks,
            habits = habits,
            completions = completions,
            now = now
        )

        AnalyticsUiState(
            selectedPeriod = params.period,
            weekStartDate = params.weekStart,
            currentMonth = params.month,
            currentYear = params.year,
            weeklyAnalytics = weekly,
            monthlyAnalytics = monthly,
            yearlyAnalytics = yearly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    fun onPreviousWeek() {
        _weekStartDate.value = _weekStartDate.value.minusWeeks(1)
    }

    fun onNextWeek() {
        _weekStartDate.value = _weekStartDate.value.plusWeeks(1)
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun onPreviousYear() {
        _currentYear.value -= 1
    }

    fun onNextYear() {
        _currentYear.value += 1
    }
}

private data class PeriodParams(
    val period: AnalyticsPeriod,
    val weekStart: LocalDate,
    val month: YearMonth,
    val year: Int
)
