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
import kotlinx.coroutines.Job
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
    val bestFocusPeriod: BestFocusPeriodRecommendation = BestFocusPeriodRecommendation.EMPTY,
    val distractionDashboard: DistractionDashboardData = DistractionDashboardData(
        totalDistractionSeconds = 0L,
        totalDistractionsCount = 0,
        totalAppSwitches = 0,
        averageDistractionSeconds = 0L,
        longestDistractionSeconds = 0L,
        mostDistractingAppName = "None",
        distractionPercentage = 0f,
        rankedApps = emptyList()
    ),
    val distractionPatterns: DistractionPatternReport = DistractionPatternReport(
        mostDistractingHourFormatted = "N/A",
        mostDistractingDay = "N/A",
        mostFrequentlyOpenedApp = "None",
        appWithLongestAverageVisit = "None",
        distractionsPerSession = 0f,
        observationSummary = "No distraction patterns detected."
    ),
    val taskEfficiency: TaskEfficiencySummary = TaskEfficiencySummary(
        averageEstimationAccuracy = 0,
        overallEstimatedMinutes = 0,
        overallActualFocusedMinutes = 0,
        completedTasksLinked = 0,
        categoryBreakdown = emptyList(),
        taskComparisons = emptyList(),
        insights = emptyList()
    ),
    val productivityInsights: List<ProductivityInsight> = emptyList()
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusSessionRepository(application)
    private val taskRepository = TaskRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val prefsManager = UserPreferencesManager(application)
    private val usageMonitor = AppUsageMonitor(AndroidAppUsageProvider(application))
    private val analyticsEngine = FocusAnalyticsEngine()
    private val advancedAnalyticsEngine = AdvancedAnalyticsEngine()
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
        viewModelScope.launch(Dispatchers.Default) {
            FocusTimerService.activeSessionSnapshot.collect { snapshot ->
                _uiState.update { it.copy(liveSnapshot = snapshot) }
            }
        }

        // Observe active session from DB
        viewModelScope.launch(Dispatchers.Default) {
            var hasTriggeredRecovery = false
            repository.getActiveSessionFlow().collect { active ->
                val currentSnapshot = _uiState.value.liveSnapshot
                val shouldRecover = active != null && !hasTriggeredRecovery && currentSnapshot.status == SessionStatus.IDLE &&
                    (active.sessionStatus == SessionStatus.ACTIVE || active.sessionStatus == SessionStatus.PAUSED)

                if (shouldRecover) {
                    hasTriggeredRecovery = true
                    FocusTimerService.recoverSession(getApplication(), active.id)
                }

                val resolvedSnapshot = if (active != null && currentSnapshot.status == SessionStatus.IDLE &&
                    (active.sessionStatus == SessionStatus.ACTIVE || active.sessionStatus == SessionStatus.PAUSED)
                ) {
                    LiveTimerSnapshot(
                        sessionId = active.id,
                        status = active.sessionStatus,
                        focusedSeconds = active.focusedDurationSeconds,
                        pausedSeconds = active.pausedDurationSeconds,
                        totalElapsedSeconds = active.totalElapsedSeconds,
                        distractedSeconds = active.distractedDurationSeconds,
                        productiveSeconds = active.productiveAppDurationSeconds,
                        targetDurationSeconds = active.targetDurationSeconds,
                        remainingSeconds = if (active.targetDurationSeconds > 0) {
                            (active.targetDurationSeconds - active.focusedDurationSeconds).coerceAtLeast(0)
                        } else 0L,
                        progress = if (active.targetDurationSeconds > 0) {
                            (active.focusedDurationSeconds.toFloat() / active.targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        distractionCount = active.distractionCount,
                        appSwitchCount = active.appSwitchCount
                    )
                } else currentSnapshot

                _uiState.update {
                    it.copy(
                        activeSession = active,
                        liveSnapshot = resolvedSnapshot
                    )
                }
                if (active != null) {
                    observeActiveSessionDetails(active.id)
                } else {
                    currentObservedSessionId = null
                    activeSessionDetailsJob?.cancel()
                }
            }
        }

        // Observe all sessions and compute basic stats
        viewModelScope.launch(Dispatchers.Default) {
            repository.getAllSessions().collect { sessions ->
                val historyFilter = _uiState.value.historyFilter
                val (filtered, daily, weekly) = withContext(Dispatchers.Default) {
                    Triple(
                        applyFilter(sessions, historyFilter),
                        analyticsEngine.computeDailyStats(sessions, LocalDate.now()),
                        analyticsEngine.computeWeeklyStats(sessions)
                    )
                }

                _uiState.update {
                    it.copy(
                        allSessions = sessions,
                        filteredSessions = filtered,
                        dailyStats = daily,
                        weeklyStats = weekly
                    )
                }
            }
        }

        // Observe tasks
        viewModelScope.launch(Dispatchers.Default) {
            taskRepository.getAllTasks().collect { tasks ->
                _uiState.update {
                    it.copy(availableTasks = tasks.filter { t -> !t.status.isCompleted() })
                }
            }
        }

        // Observe categories
        viewModelScope.launch(Dispatchers.Default) {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(availableCategories = categories) }
            }
        }

        // Observe and compute Advanced Intelligence
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                combine(repository.getAllSessions(), repository.getAllAppUsages(), ::Pair),
                combine(taskRepository.getAllTasks(), categoryRepository.getAllCategories(), ::Pair),
                dateFilterFlow
            ) { (sessions, usages), (tasks, categories), filter ->
                withContext(Dispatchers.Default) {
                    val filteredSessions = AdvancedAnalyticsEngine.filterSessionsByDate(sessions, filter)
                    val filteredUsages = AdvancedAnalyticsEngine.filterUsagesByDate(usages, filter)

                    val prevSessions = AdvancedAnalyticsEngine.getPreviousPeriodSessions(sessions, filter)
                    val prevUsages = AdvancedAnalyticsEngine.getPreviousPeriodUsages(usages, filter)

                    val score = advancedAnalyticsEngine.calculateFocusScore(filteredSessions, prevSessions)
                    val hourly = advancedAnalyticsEngine.calculateHourlyProductivity(filteredSessions)
                    val heatmap = advancedAnalyticsEngine.calculateWeeklyHeatmap(sessions)
                    val bestPeriod = advancedAnalyticsEngine.determineBestFocusPeriod(filteredSessions)
                    val distractions = advancedAnalyticsEngine.calculateDistractionIntelligence(filteredSessions, filteredUsages, prevSessions, prevUsages)
                    val patterns = advancedAnalyticsEngine.calculateDistractionPatterns(filteredUsages, filteredSessions.size)
                    val taskEff = advancedAnalyticsEngine.calculateTaskFocusComparison(tasks, categories, sessions)
                    val insights = advancedAnalyticsEngine.generateProductivityInsights(
                        filteredSessions, score, bestPeriod, distractions, taskEff
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
        viewModelScope.launch(Dispatchers.Default) {
            repository.getAllClassifications().collect { list ->
                _uiState.update { it.copy(classfications = list) }
            }
        }

        // Observe user settings
        viewModelScope.launch(Dispatchers.Default) {
            prefsManager.userSettingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(defaultDurationMinutes = settings.focusDefaultDurationMinutes)
                }
            }
        }
    }

    fun checkPermission() {
        val granted = usageMonitor.hasUsagePermission()
        _uiState.value = _uiState.value.copy(hasUsagePermission = granted)
    }

    private var activeSessionDetailsJob: Job? = null
    private var currentObservedSessionId: Long? = null

    private fun observeActiveSessionDetails(sessionId: Long) {
        if (currentObservedSessionId == sessionId && activeSessionDetailsJob?.isActive == true) {
            return
        }
        currentObservedSessionId = sessionId
        activeSessionDetailsJob?.cancel()
        activeSessionDetailsJob = viewModelScope.launch(Dispatchers.Default) {
            launch(Dispatchers.Default) {
                repository.getPausesForSession(sessionId).collect { pauses ->
                    _uiState.update { it.copy(activeSessionPauses = pauses) }
                }
            }
            launch(Dispatchers.Default) {
                repository.getAppUsagesForSession(sessionId).collect { usages ->
                    val top = withContext(Dispatchers.Default) {
                        analyticsEngine.computeAppStats(usages)
                    }
                    _uiState.update {
                        it.copy(
                            activeSessionUsages = usages,
                            topApps = top
                        )
                    }
                }
            }
            launch(Dispatchers.Default) {
                repository.getEventsForSession(sessionId).collect { events ->
                    _uiState.update { it.copy(activeSessionEvents = events) }
                }
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
        viewModelScope.launch(Dispatchers.IO) {
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
        val active = _uiState.value.activeSession
        if (active != null && _uiState.value.liveSnapshot.status == SessionStatus.IDLE) {
            FocusTimerService.recoverSession(getApplication(), active.id)
        }
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
            viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
            viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
