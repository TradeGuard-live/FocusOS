package com.naman.focusos.domain.focus

import com.naman.focusos.data.model.AppClassification
import java.time.DayOfWeek
import java.time.LocalDate

// =============================================================================
// ENUMS & TYPES
// =============================================================================

enum class ScoreCategory(val displayName: String, val minScore: Int, val maxScore: Int) {
    OUTSTANDING("Outstanding", 90, 100),
    EXCELLENT("Excellent", 80, 89),
    GOOD("Good", 70, 79),
    NEEDS_IMPROVEMENT("Needs Improvement", 60, 69),
    LOW_CONSISTENCY("Low Focus Consistency", 0, 59),
    INSUFFICIENT_DATA("Insufficient Data", 0, 0);

    companion object {
        fun fromScore(score: Int, isProvisional: Boolean = false): ScoreCategory {
            if (isProvisional) return INSUFFICIENT_DATA
            return when {
                score >= 90 -> OUTSTANDING
                score >= 80 -> EXCELLENT
                score >= 70 -> GOOD
                score >= 60 -> NEEDS_IMPROVEMENT
                else -> LOW_CONSISTENCY
            }
        }
    }
}

enum class RecommendationConfidence(val label: String) {
    LOW("Low Confidence"),
    MODERATE("Moderate Confidence"),
    HIGH("High Confidence");

    val displayName: String get() = label
}

enum class InsightType(val label: String) {
    POSITIVE_TREND("Positive Trend"),
    OPPORTUNITY("Improvement Opportunity"),
    SCHEDULING_SUGGESTION("Scheduling Suggestion"),
    TASK_EFFICIENCY("Task Efficiency")
}

enum class AnalyticsDateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    ALL("All Time");

    val displayName: String get() = label
}

// =============================================================================
// 1. FOCUS SCORE MODELS
// =============================================================================

data class FocusScoreDetail(
    val overallScore: Int,
    val category: ScoreCategory,
    val focusCompletionScore: Float,      // 0..100 (40% weight)
    val distractionControlScore: Float,   // 0..100 (30% weight)
    val consistencyScore: Float,          // 0..100 (20% weight)
    val sessionCompletionScore: Float,    // 0..100 (10% weight)
    val explanation: String,
    val positiveContributors: List<String>,
    val negativeContributors: List<String>,
    val isProvisional: Boolean,
    val sessionsIncludedCount: Int,
    val coveragePercentage: Int,
    val previousPeriodScore: Int? = null,
    val trendPercentage: Float? = null
) {
    companion object {
        val EMPTY = FocusScoreDetail(
            overallScore = 0,
            category = ScoreCategory.INSUFFICIENT_DATA,
            focusCompletionScore = 0f,
            distractionControlScore = 0f,
            consistencyScore = 0f,
            sessionCompletionScore = 0f,
            explanation = "No focus sessions recorded for this period yet.",
            positiveContributors = emptyList(),
            negativeContributors = emptyList(),
            isProvisional = true,
            sessionsIncludedCount = 0,
            coveragePercentage = 0
        )
    }
}

// =============================================================================
// 2. BEST FOCUS HOURS & HEATMAP MODELS
// =============================================================================

data class HourlyProductivity(
    val hour: Int,                          // 0..23
    val formattedHour: String,              // "5 AM", "2 PM"
    val totalFocusedMinutes: Int,
    val sessionCount: Int,
    val averageDurationMinutes: Int,
    val averageFocusScore: Int,
    val averageDistractionMinutes: Int,
    val targetCompletionPercentage: Int
)

data class HeatmapCell(
    val dayOfWeek: DayOfWeek,
    val dayName: String,                    // "Mon", "Tue"
    val hour: Int,                          // 5..23
    val formattedHour: String,              // "6 AM"
    val focusedMinutes: Int,
    val sessionCount: Int,
    val distractionMinutes: Int
)

data class BestFocusPeriodRecommendation(
    val bestHour: Int,
    val bestHourFormatted: String,
    val bestTwoHourStart: Int,
    val bestTwoHourFormatted: String,       // e.g. "6 AM – 8 AM"
    val bestDayOfWeek: String,              // e.g. "Tuesday"
    val mostConsistentTime: String,
    val leastProductiveTime: String,
    val confidence: RecommendationConfidence,
    val percentageOfWorkInBestPeriod: Int,
    val explanation: String
) {
    val startHour: Int get() = bestTwoHourStart
    val endHour: Int get() = (bestTwoHourStart + 2) % 24

    companion object {
        val EMPTY = BestFocusPeriodRecommendation(
            bestHour = 9,
            bestHourFormatted = "9 AM",
            bestTwoHourStart = 9,
            bestTwoHourFormatted = "9 AM – 11 AM",
            bestDayOfWeek = "N/A",
            mostConsistentTime = "N/A",
            leastProductiveTime = "N/A",
            confidence = RecommendationConfidence.LOW,
            percentageOfWorkInBestPeriod = 0,
            explanation = "Complete at least 5 focus sessions across 3 days to unlock personalized peak hour recommendations."
        )
    }
}

// =============================================================================
// 3. DISTRACTION INTELLIGENCE MODELS
// =============================================================================

data class DistractionDashboardData(
    val totalDistractionSeconds: Long,
    val totalDistractionsCount: Int,
    val totalAppSwitches: Int,
    val averageDistractionSeconds: Long,
    val longestDistractionSeconds: Long,
    val mostDistractingAppName: String,
    val distractionPercentage: Float,       // Distraction / (Focused + Distraction)
    val previousPeriodSeconds: Long? = null,
    val periodChangePercentage: Float? = null,
    val isImprovement: Boolean = false,
    val rankedApps: List<AppProductivityStat> = emptyList()
)

data class DistractionTimelineItem(
    val timestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long,
    val packageName: String,
    val applicationName: String,
    val classification: AppClassification,
    val sessionState: String                // "ACTIVE" vs "PAUSED"
)

data class DistractionPatternReport(
    val mostDistractingHourFormatted: String,
    val mostDistractingDay: String,
    val mostFrequentlyOpenedApp: String,
    val appWithLongestAverageVisit: String,
    val distractionsPerSession: Float,
    val observationSummary: String
)

// =============================================================================
// 4. TASK VS FOCUS COMPARISON MODELS
// =============================================================================

data class TaskFocusComparison(
    val taskId: Long,
    val taskTitle: String,
    val categoryName: String,
    val estimatedMinutes: Int,
    val actualFocusedMinutes: Int,
    val differenceMinutes: Int,             // actual - estimated
    val sessionCount: Int,
    val pausedMinutes: Int,
    val distractionMinutes: Int,
    val isCompleted: Boolean,
    val estimationAccuracyPercentage: Int   // 0..100
)

data class CategoryFocusAnalytics(
    val categoryId: Long,
    val categoryName: String,
    val categoryColorHex: String,
    val totalFocusedMinutes: Int,
    val completedTasksCount: Int,
    val averageMinutesPerTask: Int,
    val estimatedVsActualRatio: Float,
    val percentageOfTotalFocus: Int
)

data class TaskEfficiencySummary(
    val averageEstimationAccuracy: Int,
    val overallEstimatedMinutes: Int,
    val overallActualFocusedMinutes: Int,
    val completedTasksLinked: Int,
    val categoryBreakdown: List<CategoryFocusAnalytics>,
    val taskComparisons: List<TaskFocusComparison>,
    val insights: List<String>
)

// =============================================================================
// 5. SMART PRODUCTIVITY INSIGHTS
// =============================================================================

data class ProductivityInsight(
    val id: String,
    val title: String,
    val message: String,
    val evidence: String,
    val type: InsightType,
    val confidence: RecommendationConfidence
)

