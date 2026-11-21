package com.naman.focusos.domain.focus

import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class AdvancedAnalyticsEngine(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    // =========================================================================
    // FEATURE 1 — INTELLIGENT FOCUS SCORE
    // =========================================================================

    /**
     * Calculates the Focus Score (0..100) based on:
     * - Focus Completion (40%)
     * - Distraction Control (30%)
     * - Session Consistency (20%)
     * - Session Completion (10%)
     */
    fun calculateFocusScore(
        sessions: List<FocusSessionEntity>,
        previousPeriodSessions: List<FocusSessionEntity> = emptyList(),
        periodDays: Int = 7
    ): FocusScoreDetail {
        val validSessions = sessions.filter { it.sessionStatus != SessionStatus.CANCELLED }
        val totalFocusedSeconds = validSessions.sumOf { it.focusedDurationSeconds }

        // Provisional if less than 1 session or less than 15 minutes of work
        if (validSessions.isEmpty() || totalFocusedSeconds < 900L) {
            return FocusScoreDetail.EMPTY.copy(
                sessionsIncludedCount = validSessions.size,
                coveragePercentage = if (validSessions.isNotEmpty()) 100 else 0
            )
        }

        // A. Focus Completion (0..100, 40% weight)
        val completionScores = validSessions.map { s ->
            if (s.targetDurationSeconds > 0) {
                ((s.focusedDurationSeconds.toDouble() / s.targetDurationSeconds.toDouble()) * 100.0)
                    .coerceIn(0.0, 100.0)
            } else {
                // Open-ended: fair scaling where 25+ min gives 100%, shorter sessions scale linearly
                ((s.focusedDurationSeconds.toDouble() / 1500.0) * 100.0)
                    .coerceIn(0.0, 100.0)
            }
        }
        val focusCompletionScore = completionScores.average().toFloat().coerceIn(0f, 100f)

        // B. Distraction Control (0..100, 30% weight)
        val totalDistractionSeconds = validSessions.sumOf { it.distractedDurationSeconds }
        val totalActiveTime = totalFocusedSeconds + totalDistractionSeconds
        val rawDistractionRatio = if (totalActiveTime > 0) {
            (totalFocusedSeconds.toDouble() / totalActiveTime.toDouble()) * 100.0
        } else 100.0

        val totalSwitches = validSessions.sumOf { it.appSwitchCount }
        val focusHours = (totalFocusedSeconds / 3600.0).coerceAtLeast(0.5)
        val switchesPerHour = totalSwitches / focusHours
        val switchPenalty = (switchesPerHour * 1.5).coerceIn(0.0, 20.0)

        val distractionControlScore = (rawDistractionRatio - switchPenalty).toFloat().coerceIn(0f, 100f)

        // C. Session Consistency (0..100, 20% weight)
        val activeDaysCount = validSessions.map {
            Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
        }.distinct().size
        val effectiveDays = periodDays.coerceAtLeast(1)
        val dayRatioScore = ((activeDaysCount.toDouble() / effectiveDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)

        // Uniformity: sessions >= 20 mins show disciplined blocks
        val solidSessionsRatio = validSessions.count { it.focusedDurationSeconds >= 1200L }.toDouble() / validSessions.size.toDouble() * 100.0
        val consistencyScore = ((dayRatioScore * 0.6) + (solidSessionsRatio * 0.4)).toFloat().coerceIn(0f, 100f)

        // D. Session Completion (0..100, 10% weight)
        // Ratio of completed sessions to (completed + stopped early sessions)
        val completedCount = validSessions.count { it.sessionStatus == SessionStatus.COMPLETED }
        val targetSessions = validSessions.filter { it.targetDurationSeconds > 0 }
        val sessionCompletionScore = if (targetSessions.isNotEmpty()) {
            val targetCompleted = targetSessions.count { it.focusedDurationSeconds >= it.targetDurationSeconds }
            ((targetCompleted.toDouble() / targetSessions.size.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            ((completedCount.toDouble() / validSessions.size.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        }

        // Weighted Overall Score
        val weightedScore = (focusCompletionScore * 0.40f) +
                (distractionControlScore * 0.30f) +
                (consistencyScore * 0.20f) +
                (sessionCompletionScore * 0.10f)

        val overallScore = weightedScore.toInt().coerceIn(0, 100)
        val category = ScoreCategory.fromScore(overallScore, isProvisional = false)

        // Contributors
        val positiveContributors = mutableListOf<String>()
        val negativeContributors = mutableListOf<String>()

        if (focusCompletionScore >= 80f) {
            positiveContributors.add("High target completion rate (${focusCompletionScore.toInt()}%)")
        } else if (focusCompletionScore < 60f) {
            negativeContributors.add("Sessions frequently ended before planned targets (${focusCompletionScore.toInt()}%)")
        }

        if (distractionControlScore >= 85f) {
            positiveContributors.add("Excellent distraction control (${distractionControlScore.toInt()}%) with minimal app switching")
        } else if (distractionControlScore < 65f) {
            val distractionMins = (totalDistractionSeconds / 60).toInt()
            negativeContributors.add("Spent $distractionMins minutes outside focus apps during active work")
        }

        if (consistencyScore >= 75f) {
            positiveContributors.add("Solid consistency across $activeDaysCount active days")
        } else if (consistencyScore < 50f) {
            negativeContributors.add("Inconsistent focus schedule ($activeDaysCount of $effectiveDays days)")
        }

        val explanation = when (category) {
            ScoreCategory.OUTSTANDING -> "Outstanding deep work discipline with near-zero distractions!"
            ScoreCategory.EXCELLENT -> "Excellent focus performance! You achieved high target completion with strong focus retention."
            ScoreCategory.GOOD -> "Good overall productivity. Minor app switching reduced your potential score."
            ScoreCategory.NEEDS_IMPROVEMENT -> "Multiple interruptions and unreached session targets affected your score."
            ScoreCategory.LOW_CONSISTENCY -> "Focus was heavily fragmented by external application usage."
            ScoreCategory.INSUFFICIENT_DATA -> "More sessions needed for a verified score."
        }

        // Previous period trend
        var prevScore: Int? = null
        var trend: Float? = null
        if (previousPeriodSessions.isNotEmpty()) {
            val prevDetail = calculateFocusScore(previousPeriodSessions, emptyList(), periodDays)
            if (!prevDetail.isProvisional) {
                prevScore = prevDetail.overallScore
                trend = if (prevScore > 0) {
                    (((overallScore - prevScore).toFloat() / prevScore.toFloat()) * 100f)
                } else null
            }
        }

        return FocusScoreDetail(
            overallScore = overallScore,
            category = category,
            focusCompletionScore = focusCompletionScore,
            distractionControlScore = distractionControlScore,
            consistencyScore = consistencyScore,
            sessionCompletionScore = sessionCompletionScore,
            explanation = explanation,
            positiveContributors = positiveContributors,
            negativeContributors = negativeContributors,
            isProvisional = false,
            sessionsIncludedCount = validSessions.size,
            coveragePercentage = 100,
            previousPeriodScore = prevScore,
            trendPercentage = trend
        )
    }

    // =========================================================================
    // FEATURE 2 — BEST FOCUS HOURS & WEEKLY HEATMAP
    // =========================================================================

    /**
     * Divides historical focus activity into 24 hourly periods.
     */
    fun calculateHourlyProductivity(
        sessions: List<FocusSessionEntity>
    ): List<HourlyProductivity> {
        val hourlyBuckets = Array(24) { hour ->
            HourlyBucket(hour = hour)
        }

        for (s in sessions) {
            if (s.focusedDurationSeconds <= 0) continue
            val startZdt = Instant.ofEpochMilli(s.startTimestamp).atZone(zoneId)
            val startHour = startZdt.hour
            val totalMins = (s.focusedDurationSeconds / 60).toInt()
            val distractionMinutes = (s.distractedDurationSeconds / 60).toInt()

            if (totalMins > 0) {
                var currentMinuteTime = startZdt
                for (m in 0 until totalMins) {
                    val h = currentMinuteTime.hour
                    hourlyBuckets[h].totalFocusedMinutes++
                    currentMinuteTime = currentMinuteTime.plusMinutes(1)
                }
            }

            hourlyBuckets[startHour].sessionCount++
            hourlyBuckets[startHour].totalDistractionMinutes += distractionMinutes

            if (s.targetDurationSeconds > 0 && s.focusedDurationSeconds >= s.targetDurationSeconds) {
                hourlyBuckets[startHour].targetCompletedCount++
            } else if (s.targetDurationSeconds == 0L && s.focusedDurationSeconds >= 1200L) {
                hourlyBuckets[startHour].targetCompletedCount++
            }
        }

        return hourlyBuckets.map { b ->
            val avgDuration = if (b.sessionCount > 0) b.totalFocusedMinutes / b.sessionCount else 0
            val avgDistraction = if (b.sessionCount > 0) b.totalDistractionMinutes / b.sessionCount else 0
            val completionRate = if (b.sessionCount > 0) {
                ((b.targetCompletedCount.toDouble() / b.sessionCount.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            } else 0

            val focusRatio = if (b.totalFocusedMinutes + b.totalDistractionMinutes > 0) {
                (b.totalFocusedMinutes.toDouble() / (b.totalFocusedMinutes + b.totalDistractionMinutes).toDouble()) * 100.0
            } else 0.0
            val avgScore = (focusRatio * 0.7 + completionRate * 0.3).toInt().coerceIn(0, 100)

            HourlyProductivity(
                hour = b.hour,
                formattedHour = formatHourOfDay(b.hour),
                totalFocusedMinutes = b.totalFocusedMinutes,
                sessionCount = b.sessionCount,
                averageDurationMinutes = avgDuration,
                averageFocusScore = if (b.sessionCount > 0) avgScore else 0,
                averageDistractionMinutes = avgDistraction,
                targetCompletionPercentage = completionRate
            )
        }
    }

    /**
     * Computes 7x19 Heatmap data (Monday..Sunday x 5 AM..11 PM).
     */
    fun calculateWeeklyHeatmap(
        sessions: List<FocusSessionEntity>
    ): List<HeatmapCell> {
        val cellMap = mutableMapOf<Pair<DayOfWeek, Int>, MutableHeatmapCell>()

        // Initialize grid for all days Mon..Sun and hours 5..23
        for (day in DayOfWeek.entries) {
            for (hour in 5..23) {
                cellMap[day to hour] = MutableHeatmapCell(day, hour)
            }
        }

        for (s in sessions) {
            if (s.focusedDurationSeconds <= 0) continue
            val zdt = Instant.ofEpochMilli(s.startTimestamp).atZone(zoneId)
            val day = zdt.dayOfWeek
            val hour = zdt.hour

            if (hour in 5..23) {
                val cell = cellMap[day to hour]
                if (cell != null) {
                    cell.focusedMinutes += (s.focusedDurationSeconds / 60).toInt()
                    cell.sessionCount++
                    cell.distractionMinutes += (s.distractedDurationSeconds / 60).toInt()
                }
            }
        }

        return cellMap.values.map {
            HeatmapCell(
                dayOfWeek = it.dayOfWeek,
                dayName = it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                hour = it.hour,
                formattedHour = formatHourOfDay(it.hour),
                focusedMinutes = it.focusedMinutes,
                sessionCount = it.sessionCount,
                distractionMinutes = it.distractionMinutes
            )
        }
    }

    /**
     * Determines peak productivity hours with confidence thresholds.
     */
    fun determineBestFocusPeriod(
        sessions: List<FocusSessionEntity>
    ): BestFocusPeriodRecommendation {
        val validSessions = sessions.filter { it.focusedDurationSeconds >= 60L }
        val distinctDays = validSessions.map {
            Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
        }.distinct().size

        // Confidence evaluation
        val confidence = when {
            validSessions.isEmpty() -> RecommendationConfidence.LOW
            validSessions.size >= 10 && distinctDays >= 3 -> RecommendationConfidence.HIGH
            validSessions.size >= 5 && distinctDays >= 2 -> RecommendationConfidence.MODERATE
            else -> RecommendationConfidence.LOW
        }

        if (validSessions.isEmpty()) {
            return BestFocusPeriodRecommendation.EMPTY
        }

        val hourlyList = calculateHourlyProductivity(validSessions)
        val bestHourItem = hourlyList.maxByOrNull { it.totalFocusedMinutes } ?: hourlyList[9]

        // Find best 2-hour window (sum of focused minutes - distractions)
        var bestTwoHourStart = 9
        var bestTwoHourScore = -1.0
        var bestTwoHourFocusMins = 0

        for (h in 0..23) {
            val h1 = hourlyList[h]
            val h2 = hourlyList[(h + 1) % 24]
            val totalMins = h1.totalFocusedMinutes + h2.totalFocusedMinutes
            val distractionMins = h1.averageDistractionMinutes + h2.averageDistractionMinutes
            val score = totalMins - (distractionMins * 1.5)

            if (score > bestTwoHourScore) {
                bestTwoHourScore = score
                bestTwoHourStart = h
                bestTwoHourFocusMins = totalMins
            }
        }

        // Find best day of week
        val dayGroup = validSessions.groupBy {
            Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).dayOfWeek
        }
        val bestDay = dayGroup.maxByOrNull { (_, list) -> list.sumOf { it.focusedDurationSeconds } }?.key ?: DayOfWeek.TUESDAY
        val bestDayName = bestDay.getDisplayName(TextStyle.FULL, Locale.getDefault())

        // Find least productive time (among hours where user actually worked)
        val activeHours = hourlyList.filter { it.sessionCount > 0 }
        val leastProductiveHour = activeHours.minByOrNull { it.averageFocusScore }?.hour ?: 15

        val totalFocusedAll = validSessions.sumOf { it.focusedDurationSeconds } / 60
        val percentageInWindow = if (totalFocusedAll > 0) {
            ((bestTwoHourFocusMins.toDouble() / totalFocusedAll.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        val startFmt = formatHourOfDay(bestTwoHourStart)
        val endFmt = formatHourOfDay(bestTwoHourStart + 2)
        val windowFmt = "$startFmt – $endFmt"

        val explanation = when (confidence) {
            RecommendationConfidence.LOW ->
                "Recorded ${validSessions.size} session(s) across $distinctDays day(s). Preliminary peak window identified at $windowFmt. Complete more sessions across different days for higher confidence recommendations."
            else ->
                "Based on $distinctDays days of activity, you complete $percentageInWindow% of your deepest work between $windowFmt with fewer interruptions."
        }

        return BestFocusPeriodRecommendation(
            bestHour = bestHourItem.hour,
            bestHourFormatted = bestHourItem.formattedHour,
            bestTwoHourStart = bestTwoHourStart,
            bestTwoHourFormatted = windowFmt,
            bestDayOfWeek = bestDayName,
            mostConsistentTime = "$startFmt on $bestDayName",
            leastProductiveTime = formatHourOfDay(leastProductiveHour),
            confidence = confidence,
            percentageOfWorkInBestPeriod = percentageInWindow,
            explanation = explanation
        )
    }

    // =========================================================================
    // FEATURE 3 — DISTRACTION INTELLIGENCE
    // =========================================================================

    /**
     * Aggregates distraction metrics and period comparison.
     */
    fun calculateDistractionIntelligence(
        sessions: List<FocusSessionEntity>,
        usages: List<FocusAppUsageEntity>,
        previousSessions: List<FocusSessionEntity> = emptyList(),
        previousUsages: List<FocusAppUsageEntity> = emptyList()
    ): DistractionDashboardData {
        val activeDistractions = usages.filter {
            it.classification == AppClassification.DISTRACTING_APP && it.sessionState == "ACTIVE"
        }

        val totalDistractionSecs = activeDistractions.sumOf { it.durationSeconds }
        val distractionCount = activeDistractions.size
        val totalAppSwitches = sessions.sumOf { it.appSwitchCount }
        val totalFocusedSecs = sessions.sumOf { it.focusedDurationSeconds }

        val avgDistractionSecs = if (distractionCount > 0) totalDistractionSecs / distractionCount else 0L
        val longestSecs = activeDistractions.maxOfOrNull { it.durationSeconds } ?: 0L

        val mostDistracting = activeDistractions
            .groupBy { it.applicationName }
            .maxByOrNull { (_, list) -> list.sumOf { it.durationSeconds } }?.key ?: "None"

        val totalActiveSecs = totalFocusedSecs + totalDistractionSecs
        val distractionPct = if (totalActiveSecs > 0) {
            ((totalDistractionSecs.toFloat() / totalActiveSecs.toFloat()) * 100f).coerceIn(0f, 100f)
        } else 0f

        // Previous period comparison
        val prevActiveDistractions = previousUsages.filter {
            it.classification == AppClassification.DISTRACTING_APP && it.sessionState == "ACTIVE"
        }
        val prevDistractionSecs = prevActiveDistractions.sumOf { it.durationSeconds }

        var periodChange: Float? = null
        var isImprovement = false
        if (previousSessions.isNotEmpty()) {
            if (prevDistractionSecs > 0) {
                val diff = totalDistractionSecs - prevDistractionSecs
                periodChange = (diff.toFloat() / prevDistractionSecs.toFloat()) * 100f
                isImprovement = periodChange < 0f
            } else if (totalDistractionSecs == 0L) {
                periodChange = 0f
                isImprovement = true
            } else {
                periodChange = 100f
                isImprovement = false
            }
        }

        val rankedApps = FocusAnalyticsEngine().computeAppStats(usages)

        return DistractionDashboardData(
            totalDistractionSeconds = totalDistractionSecs,
            totalDistractionsCount = distractionCount,
            totalAppSwitches = totalAppSwitches,
            averageDistractionSeconds = avgDistractionSecs,
            longestDistractionSeconds = longestSecs,
            mostDistractingAppName = mostDistracting,
            distractionPercentage = distractionPct,
            previousPeriodSeconds = if (previousSessions.isNotEmpty()) prevDistractionSecs else null,
            periodChangePercentage = periodChange,
            isImprovement = isImprovement,
            rankedApps = rankedApps
        )
    }

    /**
     * Identifies distraction patterns (peak distracting hour, day, average visits).
     */
    fun calculateDistractionPatterns(
        usages: List<FocusAppUsageEntity>,
        sessionCount: Int
    ): DistractionPatternReport {
        val distractions = usages.filter {
            it.classification == AppClassification.DISTRACTING_APP && it.sessionState == "ACTIVE"
        }

        if (distractions.isEmpty()) {
            return DistractionPatternReport(
                mostDistractingHourFormatted = "None",
                mostDistractingDay = "None",
                mostFrequentlyOpenedApp = "None",
                appWithLongestAverageVisit = "None",
                distractionsPerSession = 0f,
                observationSummary = "Zero distractions recorded during active focus sessions."
            )
        }

        val hourDistractions = distractions.groupBy {
            Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).hour
        }
        val peakHour = hourDistractions.maxByOrNull { (_, list) -> list.sumOf { it.durationSeconds } }?.key ?: 14

        val dayDistractions = distractions.groupBy {
            Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).dayOfWeek
        }
        val peakDay = dayDistractions.maxByOrNull { (_, list) -> list.sumOf { it.durationSeconds } }?.key ?: DayOfWeek.FRIDAY
        val peakDayName = peakDay.getDisplayName(TextStyle.FULL, Locale.getDefault())

        val appGroups = distractions.groupBy { it.applicationName }
        val mostFrequentApp = appGroups.maxByOrNull { (_, list) -> list.size }?.key ?: "None"

        val longestAvgApp = appGroups.maxByOrNull { (_, list) ->
            if (list.isNotEmpty()) list.sumOf { it.durationSeconds } / list.size else 0L
        }?.key ?: "None"

        val perSession = if (sessionCount > 0) {
            distractions.size.toFloat() / sessionCount.toFloat()
        } else 0f

        val summary = "You experienced the most interruptions on $peakDayName around ${formatHourOfDay(peakHour)}, with $mostFrequentApp being the most frequently accessed application."

        return DistractionPatternReport(
            mostDistractingHourFormatted = formatHourOfDay(peakHour),
            mostDistractingDay = peakDayName,
            mostFrequentlyOpenedApp = mostFrequentApp,
            appWithLongestAverageVisit = longestAvgApp,
            distractionsPerSession = perSession,
            observationSummary = summary
        )
    }

    // =========================================================================
    // FEATURE 4 — TASK VS FOCUS COMPARISON
    // =========================================================================

    /**
     * Compares planned task estimates with actual focused work.
     */
    fun calculateTaskFocusComparison(
        tasks: List<TaskEntity>,
        categories: List<CategoryEntity>,
        sessions: List<FocusSessionEntity>
    ): TaskEfficiencySummary {
        val categoryMap = categories.associateBy { it.id }
        val linkedSessionsMap = sessions.filter { it.linkedTaskId != null }.groupBy { it.linkedTaskId!! }

        val taskComparisons = mutableListOf<TaskFocusComparison>()
        val categoryFocusMinutes = mutableMapOf<Long, Int>()
        val categoryCompletedCount = mutableMapOf<Long, Int>()
        val categoryEstimatedMinutes = mutableMapOf<Long, Int>()

        for (task in tasks) {
            val linked = linkedSessionsMap[task.id] ?: emptyList()
            val actualSecs = linked.sumOf { it.focusedDurationSeconds }
            val actualMins = (actualSecs / 60).toInt()
            val estimatedMins = task.durationMinutes
            val diffMins = actualMins - estimatedMins
            val pausedMins = (linked.sumOf { it.pausedDurationSeconds } / 60).toInt()
            val distractionMins = (linked.sumOf { it.distractedDurationSeconds } / 60).toInt()

            val accuracy = calculateEstimationAccuracy(estimatedMins, actualMins)
            val cat = categoryMap[task.categoryId]
            val catName = cat?.name ?: "General"

            // Accumulate category stats
            val currentCatFocus = categoryFocusMinutes.getOrDefault(task.categoryId, 0)
            categoryFocusMinutes[task.categoryId] = currentCatFocus + actualMins

            val currentCatEst = categoryEstimatedMinutes.getOrDefault(task.categoryId, 0)
            categoryEstimatedMinutes[task.categoryId] = currentCatEst + estimatedMins

            if (task.status.isCompleted()) {
                val currentComp = categoryCompletedCount.getOrDefault(task.categoryId, 0)
                categoryCompletedCount[task.categoryId] = currentComp + 1
            }

            if (linked.isNotEmpty()) {
                taskComparisons.add(
                    TaskFocusComparison(
                        taskId = task.id,
                        taskTitle = task.title,
                        categoryName = catName,
                        estimatedMinutes = estimatedMins,
                        actualFocusedMinutes = actualMins,
                        differenceMinutes = diffMins,
                        sessionCount = linked.size,
                        pausedMinutes = pausedMins,
                        distractionMinutes = distractionMins,
                        isCompleted = task.status.isCompleted(),
                        estimationAccuracyPercentage = accuracy
                    )
                )
            }
        }

        val totalActualMinsAll = categoryFocusMinutes.values.sum()
        val categoryAnalyticsList = categories.map { cat ->
            val focusedMins = categoryFocusMinutes.getOrDefault(cat.id, 0)
            val completed = categoryCompletedCount.getOrDefault(cat.id, 0)
            val estimated = categoryEstimatedMinutes.getOrDefault(cat.id, 0)
            val avgMins = if (completed > 0) focusedMins / completed else focusedMins
            val ratio = if (estimated > 0) focusedMins.toFloat() / estimated.toFloat() else 1.0f
            val pctOfTotal = if (totalActualMinsAll > 0) ((focusedMins.toDouble() / totalActualMinsAll.toDouble()) * 100.0).toInt() else 0

            CategoryFocusAnalytics(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryColorHex = cat.colorHex,
                totalFocusedMinutes = focusedMins,
                completedTasksCount = completed,
                averageMinutesPerTask = avgMins,
                estimatedVsActualRatio = ratio,
                percentageOfTotalFocus = pctOfTotal
            )
        }.sortedByDescending { it.totalFocusedMinutes }

        val avgAccuracy = if (taskComparisons.isNotEmpty()) {
            taskComparisons.map { it.estimationAccuracyPercentage }.average().toInt()
        } else 100

        val overallEstimated = taskComparisons.sumOf { it.estimatedMinutes }
        val overallActual = taskComparisons.sumOf { it.actualFocusedMinutes }

        // Generate evidence-based insights
        val insights = mutableListOf<String>()
        val topCat = categoryAnalyticsList.firstOrNull { it.totalFocusedMinutes > 0 }
        if (topCat != null) {
            val hrs = String.format("%.1f", topCat.totalFocusedMinutes / 60f)
            insights.add("You spent $hrs hours on ${topCat.categoryName} (${topCat.percentageOfTotalFocus}% of total focus).")
        }

        if (taskComparisons.size >= 3) {
            val diff = overallActual - overallEstimated
            if (diff > 15) {
                val pctOver = ((diff.toDouble() / overallEstimated.toDouble()) * 100.0).toInt().coerceAtMost(100)
                insights.add("Your tasks took approximately $pctOver% longer than your initial estimates.")
            } else if (diff < -15) {
                val pctUnder = ((-diff.toDouble() / overallEstimated.toDouble()) * 100.0).toInt().coerceAtMost(100)
                insights.add("You completed tasks $pctUnder% faster than your estimated duration.")
            } else {
                insights.add("Your task time estimation accuracy is within a tight 10% tolerance.")
            }
        }

        return TaskEfficiencySummary(
            averageEstimationAccuracy = avgAccuracy,
            overallEstimatedMinutes = overallEstimated,
            overallActualFocusedMinutes = overallActual,
            completedTasksLinked = taskComparisons.count { it.isCompleted },
            categoryBreakdown = categoryAnalyticsList,
            taskComparisons = taskComparisons,
            insights = insights
        )
    }

    /**
     * Symmetric estimation accuracy calculation:
     * - Returns 100% if estimated == actual
     * - Returns min(est, act) / max(est, act) * 100
     * Bounded between 0% and 100%.
     */
    fun calculateEstimationAccuracy(estimatedMins: Int, actualMins: Int): Int {
        if (estimatedMins <= 0 && actualMins <= 0) return 100
        if (estimatedMins <= 0 || actualMins <= 0) return 0
        val minVal = minOf(estimatedMins, actualMins).toDouble()
        val maxVal = maxOf(estimatedMins, actualMins).toDouble()
        return ((minVal / maxVal) * 100.0).toInt().coerceIn(0, 100)
    }

    // =========================================================================
    // FEATURE 5 — PRODUCTIVITY INSIGHTS ENGINE
    // =========================================================================

    /**
     * Combines all 4 analytical features to produce actionable, evidence-based insights.
     */
    fun generateProductivityInsights(
        sessions: List<FocusSessionEntity>,
        scoreDetail: FocusScoreDetail,
        bestPeriod: BestFocusPeriodRecommendation,
        distractionData: DistractionDashboardData,
        taskEfficiency: TaskEfficiencySummary
    ): List<ProductivityInsight> {
        val insights = mutableListOf<ProductivityInsight>()

        // 1. Peak focus hours insight
        if (bestPeriod.confidence != RecommendationConfidence.LOW) {
            insights.add(
                ProductivityInsight(
                    id = "peak_hours",
                    title = "Optimal Deep Work Window",
                    message = "Your highest focus scores and longest uninterrupted sessions occur between ${bestPeriod.bestTwoHourFormatted}.",
                    evidence = "Historical focus data shows ${bestPeriod.percentageOfWorkInBestPeriod}% of completed work in this window.",
                    type = InsightType.SCHEDULING_SUGGESTION,
                    confidence = bestPeriod.confidence
                )
            )
        }

        // 2. Distraction trend insight
        if (distractionData.periodChangePercentage != null) {
            val pct = Math.abs(distractionData.periodChangePercentage).toInt()
            if (distractionData.isImprovement && pct > 5) {
                insights.add(
                    ProductivityInsight(
                        id = "distraction_improvement",
                        title = "Reduced Interruptions",
                        message = "Your total distraction time decreased by $pct% compared to the previous period.",
                        evidence = "Distraction duration dropped from ${distractionData.previousPeriodSeconds?.div(60) ?: 0}m to ${distractionData.totalDistractionSeconds / 60}m.",
                        type = InsightType.POSITIVE_TREND,
                        confidence = RecommendationConfidence.HIGH
                    )
                )
            } else if (!distractionData.isImprovement && pct > 10) {
                insights.add(
                    ProductivityInsight(
                        id = "distraction_increase",
                        title = "Distraction Opportunity",
                        message = "Distractions increased by $pct% during recent sessions, primarily driven by ${distractionData.mostDistractingAppName}.",
                        evidence = "Logged ${distractionData.totalDistractionsCount} interruptions across ${distractionData.totalAppSwitches} app switches.",
                        type = InsightType.OPPORTUNITY,
                        confidence = RecommendationConfidence.MODERATE
                    )
                )
            }
        }

        // 3. Focus score discipline insight
        if (!scoreDetail.isProvisional && scoreDetail.overallScore >= 80) {
            insights.add(
                ProductivityInsight(
                    id = "high_focus_score",
                    title = "Strong Session Discipline",
                    message = "You earned a ${scoreDetail.category.displayName} score (${scoreDetail.overallScore}/100) across ${scoreDetail.sessionsIncludedCount} sessions.",
                    evidence = "Target completion was ${scoreDetail.focusCompletionScore.toInt()}% with a ${scoreDetail.distractionControlScore.toInt()}% distraction control score.",
                    type = InsightType.POSITIVE_TREND,
                    confidence = RecommendationConfidence.HIGH
                )
            )
        }

        // 4. Task estimation accuracy insight
        if (taskEfficiency.taskComparisons.size >= 3) {
            val acc = taskEfficiency.averageEstimationAccuracy
            insights.add(
                ProductivityInsight(
                    id = "task_accuracy",
                    title = "Task Planning Accuracy",
                    message = "Your average estimation accuracy across ${taskEfficiency.taskComparisons.size} linked tasks is $acc%.",
                    evidence = "Total planned: ${taskEfficiency.overallEstimatedMinutes}m vs actual: ${taskEfficiency.overallActualFocusedMinutes}m.",
                    type = InsightType.TASK_EFFICIENCY,
                    confidence = RecommendationConfidence.MODERATE
                )
            )
        }

        // 5. Category breakdown insight
        val topCat = taskEfficiency.categoryBreakdown.firstOrNull { it.totalFocusedMinutes > 60 }
        if (topCat != null) {
            insights.add(
                ProductivityInsight(
                    id = "category_lead",
                    title = "Primary Focus Allocation",
                    message = "${topCat.categoryName} is your most active domain with ${topCat.totalFocusedMinutes / 60}h of focused work.",
                    evidence = "Accounts for ${topCat.percentageOfTotalFocus}% of total recorded focus time.",
                    type = InsightType.POSITIVE_TREND,
                    confidence = RecommendationConfidence.HIGH
                )
            )
        }

        return insights
    }

    private fun formatHourOfDay(hour: Int): String {
        val h = hour % 24
        val amPm = if (h < 12) "AM" else "PM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$displayHour $amPm"
    }

    private class HourlyBucket(
        val hour: Int,
        var totalFocusedMinutes: Int = 0,
        var sessionCount: Int = 0,
        var totalDistractionMinutes: Int = 0,
        var targetCompletedCount: Int = 0
    )

    private class MutableHeatmapCell(
        val dayOfWeek: DayOfWeek,
        val hour: Int,
        var focusedMinutes: Int = 0,
        var sessionCount: Int = 0,
        var distractionMinutes: Int = 0
    )

    companion object {
        fun filterSessionsByDate(
            sessions: List<FocusSessionEntity>,
            filter: AnalyticsDateFilter,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<FocusSessionEntity> {
            val today = LocalDate.now(zoneId)
            return when (filter) {
                AnalyticsDateFilter.ALL -> sessions
                AnalyticsDateFilter.TODAY -> sessions.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today
                }
                AnalyticsDateFilter.YESTERDAY -> sessions.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(1)
                }
                AnalyticsDateFilter.LAST_7_DAYS -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    !d.isBefore(today.minusDays(7))
                }
                AnalyticsDateFilter.LAST_30_DAYS -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    !d.isBefore(today.minusDays(30))
                }
                AnalyticsDateFilter.THIS_MONTH -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.year == today.year && d.month == today.month
                }
            }
        }

        fun filterUsagesByDate(
            usages: List<FocusAppUsageEntity>,
            filter: AnalyticsDateFilter,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<FocusAppUsageEntity> {
            val today = LocalDate.now(zoneId)
            return when (filter) {
                AnalyticsDateFilter.ALL -> usages
                AnalyticsDateFilter.TODAY -> usages.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today
                }
                AnalyticsDateFilter.YESTERDAY -> usages.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(1)
                }
                AnalyticsDateFilter.LAST_7_DAYS -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    !d.isBefore(today.minusDays(7))
                }
                AnalyticsDateFilter.LAST_30_DAYS -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    !d.isBefore(today.minusDays(30))
                }
                AnalyticsDateFilter.THIS_MONTH -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.year == today.year && d.month == today.month
                }
            }
        }

        fun getPreviousPeriodSessions(
            sessions: List<FocusSessionEntity>,
            filter: AnalyticsDateFilter,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<FocusSessionEntity> {
            val today = LocalDate.now(zoneId)
            return when (filter) {
                AnalyticsDateFilter.TODAY -> sessions.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(1)
                }
                AnalyticsDateFilter.YESTERDAY -> sessions.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(2)
                }
                AnalyticsDateFilter.LAST_7_DAYS -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.isBefore(today.minusDays(7)) && !d.isBefore(today.minusDays(14))
                }
                AnalyticsDateFilter.LAST_30_DAYS -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.isBefore(today.minusDays(30)) && !d.isBefore(today.minusDays(60))
                }
                AnalyticsDateFilter.THIS_MONTH -> sessions.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    val lastMonth = today.minusMonths(1)
                    d.year == lastMonth.year && d.month == lastMonth.month
                }
                AnalyticsDateFilter.ALL -> emptyList()
            }
        }

        fun getPreviousPeriodUsages(
            usages: List<FocusAppUsageEntity>,
            filter: AnalyticsDateFilter,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<FocusAppUsageEntity> {
            val today = LocalDate.now(zoneId)
            return when (filter) {
                AnalyticsDateFilter.TODAY -> usages.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(1)
                }
                AnalyticsDateFilter.YESTERDAY -> usages.filter {
                    Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == today.minusDays(2)
                }
                AnalyticsDateFilter.LAST_7_DAYS -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.isBefore(today.minusDays(7)) && !d.isBefore(today.minusDays(14))
                }
                AnalyticsDateFilter.LAST_30_DAYS -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    d.isBefore(today.minusDays(30)) && !d.isBefore(today.minusDays(60))
                }
                AnalyticsDateFilter.THIS_MONTH -> usages.filter {
                    val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
                    val lastMonth = today.minusMonths(1)
                    d.year == lastMonth.year && d.month == lastMonth.month
                }
                AnalyticsDateFilter.ALL -> emptyList()
            }
        }
    }
}

