package com.naman.focusos.ui.screens.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.preferences.UserPreferencesManager
import com.naman.focusos.data.repository.CategoryRepository
import com.naman.focusos.data.repository.FocusSessionRepository
import com.naman.focusos.data.repository.TaskRepository
import com.naman.focusos.domain.focus.AdvancedAnalyticsEngine
import com.naman.focusos.domain.focus.AnalyticsDateFilter
import com.naman.focusos.domain.focus.AndroidAppUsageProvider
import com.naman.focusos.domain.focus.AppProductivityStat
import com.naman.focusos.domain.focus.AppUsageMonitor
import com.naman.focusos.domain.focus.BestFocusPeriodRecommendation
import com.naman.focusos.domain.focus.DailyFocusStats
import com.naman.focusos.domain.focus.DistractionDashboardData
import com.naman.focusos.domain.focus.DistractionPatternReport
import com.naman.focusos.domain.focus.FocusAnalyticsEngine
import com.naman.focusos.domain.focus.FocusScoreDetail
import com.naman.focusos.domain.focus.HeatmapCell
import com.naman.focusos.domain.focus.HourlyProductivity
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.domain.focus.ProductivityInsight
import com.naman.focusos.domain.focus.RecommendationConfidence
import com.naman.focusos.domain.focus.TaskEfficiencySummary
import com.naman.focusos.domain.focus.WeeklyFocusStats
import com.naman.focusos.service.FocusTimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class FocusTab(val title: String) {
    TIMER("Timer"),
    HISTORY("History"),
    ANALYTICS("Analytics")
}

enum class HistoryFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    ALL("All")
}

data class FocusUiState(
    val selectedTab: FocusTab = FocusTab.TIMER,
    val liveSnapshot: LiveTimerSnapshot = LiveTimerSnapshot.IDLE,
    val activeSession: FocusSessionEntity? = null,
    val hasUsagePermission: Boolean = false,
    val availableTasks: List<TaskEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val allSessions: List<FocusSessionEntity> = emptyList(),
    val filteredSessions: List<FocusSessionEntity> = emptyList(),
    val activeSessionPauses: List<FocusPauseEntity> = emptyList(),
    val activeSessionUsages: List<FocusAppUsageEntity> = emptyList(),
    val activeSessionEvents: List<FocusSessionEventEntity> = emptyList(),
    val inspectedSession: FocusSessionEntity? = null,
    val inspectedSessionPauses: List<FocusPauseEntity> = emptyList(),
    val inspectedSessionUsages: List<FocusAppUsageEntity> = emptyList(),
    val inspectedSessionEvents: List<FocusSessionEventEntity> = emptyList(),
    val dailyStats: DailyFocusStats? = null,
    val weeklyStats: WeeklyFocusStats? = null,
    val topApps: List<AppProductivityStat> = emptyList(),
    val classfications: List<AppClassificationEntity> = emptyList(),
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val showCompletionDialog: Boolean = false,
    val completionReportSnapshot: LiveTimerSnapshot? = null,
    val completedSessionId: Long? = null,
    val showStopConfirmation: Boolean = false,
    val showPermissionOnboarding: Boolean = false,
    val defaultDurationMinutes: Int = 25,
    // Advanced Productivity Intelligence State
    val analyticsDateFilter: AnalyticsDateFilter = AnalyticsDateFilter.LAST_7_DAYS,
    val selectedHeatmapCell: HeatmapCell? = null,
    val focusScore: FocusScoreDetail = FocusScoreDetail.EMPTY,
    val hourlyProductivity: List<HourlyProductivity> = emptyList(),
    val weeklyHeatmap: List<HeatmapCell> = emptyList(),
    val bestFocusPeriod: BestFocusPeriodRecommendation = BestFocusPeriodRecommendation(
        startHour = 9,
        endHour = 11,
        avgFocusedMinutes = 0,
        score = 0,
        confidence = RecommendationConfidence.LOW,
        description = "Insufficient data",
        recommendation = "Complete more sessions to identify your peak hours"
    ),
    val distractionDashboard: DistractionDashboardData = DistractionDashboardData(
        totalDistractionDurationSeconds = 0,
        totalDistractionCount = 0,
        totalAppSwitches = 0,
        avgDistractionDurationSeconds = 0,
        longestDistractionDurationSeconds = 0,
        distractionPercentage = 0f,
        rankedApps = emptyList()
    ),
    val distractionPatterns: DistractionPatternReport = DistractionPatternReport(),
    val taskEfficiency: TaskEfficiencySummary = TaskEfficiencySummary(0, 0, 0, 0, 0f, emptyList(), emptyList()),
    val productivityInsights: List<ProductivityInsight> = emptyList()
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusSessionRepository(application)
    private val taskRepository = TaskRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val prefsManager = UserPreferencesManager(application)
    private val usageMonitor = AppUsageMonitor(AndroidAppUsageProvider(application))
    private val analyticsEngine = FocusAnalyticsEngine()
    private val dateFilterFlow = MutableStateFlow(AnalyticsDateFilter.LAST_7_DAYS)

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    init {
        // Seed default app classifications and load data
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultClassifications()
            checkPermission()
        }

        // Observe live timer from service
        viewModelScope.launch {
            FocusTimerService.activeSessionSnapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(liveSnapshot = snapshot)
            }
        }

        // Observe active session from DB
        viewModelScope.launch {
            repository.getActiveSessionFlow().collect { active ->
                _uiState.value = _uiState.value.copy(activeSession = active)
                if (active != null) {
                    observeActiveSessionDetails(active.id)
                }
            }
        }

        // Observe all sessions and compute basic stats
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                val filtered = applyFilter(sessions, _uiState.value.historyFilter)
                val daily = analyticsEngine.computeDailyStats(sessions, LocalDate.now())
                val weekly = analyticsEngine.computeWeeklyStats(sessions)

                _uiState.value = _uiState.value.copy(
                    allSessions = sessions,
                    filteredSessions = filtered,
                    dailyStats = daily,
                    weeklyStats = weekly
                )
            }
        }

        // Observe tasks
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                _uiState.value = _uiState.value.copy(
                    availableTasks = tasks.filter { !it.status.isCompleted() }
                )
            }
        }

        // Observe categories
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(availableCategories = categories)
            }
        }

        // Observe and compute Advanced Intelligence
        viewModelScope.launch {
            combine(
                combine(repository.getAllSessions(), repository.getAllAppUsages(), ::Pair),
                combine(taskRepository.getAllTasks(), categoryRepository.getAllCategories(), ::Pair),
                dateFilterFlow
            ) { (sessions, usages), (tasks, categories), filter ->
                withContext(Dispatchers.Default) {
                    val filteredSessions = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, filter)
                    val filteredUsages = AdvancedAnalyticsEngine.filterUsagesByDate(usages, filter)

                    val prevFilter = AdvancedAnalyticsEngine.getPreviousPeriodFilter(filter)
                    val prevSessions = prevFilter?.let { AdvancedAnalyticsEngine.filterSessionsByDate(sessions, it) } ?: emptyList()
                    val prevUsages = prevFilter?.let { AdvancedAnalyticsEngine.filterUsagesByDate(usages, it) } ?: emptyList()

                    val score = AdvancedAnalyticsEngine.calculateFocusScore(filteredSessions, filteredUsages)
                    val hourly = AdvancedAnalyticsEngine.calculateHourlyProductivity(filteredSessions)
                    val heatmap = AdvancedAnalyticsEngine.generateWeeklyHeatmap(sessions)
                    val bestPeriod = AdvancedAnalyticsEngine.identifyBestFocusPeriod(hourly, filteredSessions.size)
                    val distractions = AdvancedAnalyticsEngine.calculateDistractionDashboard(filteredSessions, filteredUsages, prevSessions, prevUsages)
                    val patterns = AdvancedAnalyticsEngine.analyzeDistractionPatterns(filteredSessions, filteredUsages)
                    val taskEff = AdvancedAnalyticsEngine.calculateTaskFocusComparison(tasks, categories, sessions)
                    val insights = AdvancedAnalyticsEngine.generateProductivityInsights(
                        filteredSessions, filteredUsages, score, bestPeriod, distractions, taskEff, filter
                    )

                    _uiState.update { current ->
                        current.copy(
                            analyticsDateFilter = filter,
                            focusScore = score,
                            hourlyProductivity = hourly,
                            weeklyHeatmap = heatmap,
                            bestFocusPeriod = bestPeriod,
                            distractionDashboard = distractions,
                            distractionPatterns = patterns,
                            taskEfficiency = taskEff,
                            productivityInsights = insights
                        )
                    }
                }
            }.collect()
        }

        // Observe classifications
        viewModelScope.launch {
            repository.getAllClassifications().collect { list ->
                _uiState.value = _uiState.value.copy(classfications = list)
            }
        }

        // Observe user settings
        viewModelScope.launch {
            prefsManager.userSettingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    defaultDurationMinutes = settings.focusDefaultDurationMinutes
                )
            }
        }
    }

    fun checkPermission() {
        val granted = usageMonitor.hasUsagePermission()
        _uiState.value = _uiState.value.copy(hasUsagePermission = granted)
    }

    private fun observeActiveSessionDetails(sessionId: Long) {
        viewModelScope.launch {
            repository.getPausesForSession(sessionId).collect { pauses ->
                _uiState.value = _uiState.value.copy(activeSessionPauses = pauses)
            }
        }
        viewModelScope.launch {
            repository.getAppUsagesForSession(sessionId).collect { usages ->
                _uiState.value = _uiState.value.copy(
                    activeSessionUsages = usages,
                    topApps = analyticsEngine.computeAppStats(usages)
                )
            }
        }
        viewModelScope.launch {
            repository.getEventsForSession(sessionId).collect { events ->
                _uiState.value = _uiState.value.copy(activeSessionEvents = events)
            }
        }
    }

    fun startSession(
        title: String,
        description: String?,
        linkedTaskId: Long?,
        category: String?,
        targetDurationMinutes: Int,
        monitoringEnabled: Boolean
    ) {
        viewModelScope.launch {
            val targetSecs = targetDurationMinutes.toLong() * 60L
            val sessionId = repository.startSession(
                title = title,
                description = description,
                linkedTaskId = linkedTaskId,
                category = category,
                targetDurationSeconds = targetSecs,
                monitoringEnabled = monitoringEnabled
            )

            FocusTimerService.startSession(
                context = getApplication(),
                sessionId = sessionId,
                title = title.ifBlank { "Focus Session" },
                targetDurationSeconds = targetSecs,
                monitoringEnabled = monitoringEnabled
            )
        }
    }

    fun pauseSession() {
        FocusTimerService.pauseSession(getApplication())
    }

    fun resumeSession() {
        FocusTimerService.resumeSession(getApplication())
    }

    fun requestStopSession() {
        _uiState.value = _uiState.value.copy(showStopConfirmation = true)
    }

    fun dismissStopConfirmation() {
        _uiState.value = _uiState.value.copy(showStopConfirmation = false)
    }

    fun confirmStopSession() {
        _uiState.value = _uiState.value.copy(showStopConfirmation = false)
        val snapshot = _uiState.value.liveSnapshot
        val currentSessionId = _uiState.value.activeSession?.id ?: snapshot.sessionId
        FocusTimerService.stopSession(getApplication())

        // Show completion report
        _uiState.value = _uiState.value.copy(
            showCompletionDialog = true,
            completionReportSnapshot = snapshot,
            completedSessionId = currentSessionId
        )
    }

    fun completeSession() {
        val snapshot = _uiState.value.liveSnapshot
        val currentSessionId = _uiState.value.activeSession?.id ?: snapshot.sessionId
        FocusTimerService.completeSession(getApplication())

        _uiState.value = _uiState.value.copy(
            showCompletionDialog = true,
            completionReportSnapshot = snapshot,
            completedSessionId = currentSessionId
        )
    }

    fun dismissCompletionDialog(markLinkedTaskAsCompleted: Boolean = false) {
        val sessionId = _uiState.value.completedSessionId
        val snapshot = _uiState.value.completionReportSnapshot
        if (sessionId != null && snapshot != null) {
            viewModelScope.launch {
                repository.completeSession(sessionId, snapshot, markLinkedTaskAsCompleted)
            }
        }
        _uiState.value = _uiState.value.copy(
            showCompletionDialog = false,
            completionReportSnapshot = null,
            completedSessionId = null
        )
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_uiState.value.inspectedSession?.id == sessionId) {
                _uiState.value = _uiState.value.copy(
                    inspectedSession = null,
                    inspectedSessionPauses = emptyList(),
                    inspectedSessionUsages = emptyList(),
                    inspectedSessionEvents = emptyList()
                )
            }
        }
    }

    fun inspectSession(session: FocusSessionEntity?) {
        _uiState.value = _uiState.value.copy(inspectedSession = session)
        if (session != null) {
            viewModelScope.launch {
                val pauses = repository.getPausesForSessionSync(session.id)
                val usages = repository.getAppUsagesForSessionSync(session.id)
                val events = repository.getEventsForSessionSync(session.id)
                _uiState.value = _uiState.value.copy(
                    inspectedSessionPauses = pauses,
                    inspectedSessionUsages = usages,
                    inspectedSessionEvents = events
                )
            }
        }
    }

    fun setTab(tab: FocusTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        val filtered = applyFilter(_uiState.value.allSessions, filter)
        _uiState.value = _uiState.value.copy(
            historyFilter = filter,
            filteredSessions = filtered
        )
    }

    fun updateClassification(
        packageName: String,
        appName: String,
        classification: AppClassification,
        isExcluded: Boolean = false
    ) {
        viewModelScope.launch {
            repository.updateAppClassification(packageName, appName, classification, isExcluded)
        }
    }

    fun showPermissionOnboarding(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPermissionOnboarding = show)
    }

    fun setAnalyticsDateFilter(filter: AnalyticsDateFilter) {
        dateFilterFlow.value = filter
    }

    fun selectHeatmapCell(cell: HeatmapCell?) {
        _uiState.update { it.copy(selectedHeatmapCell = cell) }
    }

    private fun applyFilter(sessions: List<FocusSessionEntity>, filter: HistoryFilter): List<FocusSessionEntity> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return when (filter) {
            HistoryFilter.ALL -> sessions
            HistoryFilter.TODAY -> sessions.filter {
                Instant.ofEpochMilli(it.startTimestamp).atZone(zone).toLocalDate() == today
            }
            HistoryFilter.YESTERDAY -> sessions.filter {
                Instant.ofEpochMilli(it.startTimestamp).atZone(zone).toLocalDate() == today.minusDays(1)
            }
            HistoryFilter.LAST_7_DAYS -> sessions.filter {
                val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zone).toLocalDate()
                !d.isBefore(today.minusDays(7))
            }
            HistoryFilter.LAST_30_DAYS -> sessions.filter {
                val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zone).toLocalDate()
                !d.isBefore(today.minusDays(30))
            }
        }
    }
}
