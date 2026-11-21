package com.naman.focusos.domain.focus

import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusSessionEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyFocusStats(
    val date: LocalDate,
    val totalFocusedMinutes: Int,
    val sessionCount: Int,
    val averageSessionDurationMinutes: Int,
    val longestSessionMinutes: Int,
    val totalPausedMinutes: Int,
    val totalDistractionMinutes: Int,
    val appSwitchCount: Int,
    val goalCompletionPercentage: Int
)

data class WeeklyDayBar(
    val dayOfWeek: DayOfWeek,
    val dayName: String,
    val focusHours: Float,
    val isToday: Boolean
)

data class WeeklyFocusStats(
    val totalWeeklyHours: Float,
    val dailyBreakdown: List<WeeklyDayBar>,
    val averageDailyMinutes: Int,
    val bestDayName: String,
    val bestDayHours: Float,
    val consistencyPercentage: Int
)

data class AppProductivityStat(
    val packageName: String,
    val applicationName: String,
    val totalDurationSeconds: Long,
    val visitCount: Int,
    val classification: AppClassification,
    val isDistraction: Boolean
)

data class MonthlyFocusStats(
    val totalMonthlyHours: Float,
    val completedSessionsCount: Int,
    val averageSessionMinutes: Int,
    val goalCompletionPercentage: Int
)

class FocusAnalyticsEngine(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun computeDailyStats(
        sessions: List<FocusSessionEntity>,
        targetDate: LocalDate = LocalDate.now(),
        dailyGoalMinutes: Int = 120
    ): DailyFocusStats {
        val daysSessions = sessions.filter {
            val sessionDate = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
            sessionDate == targetDate
        }

        if (daysSessions.isEmpty()) {
            return DailyFocusStats(
                date = targetDate,
                totalFocusedMinutes = 0,
                sessionCount = 0,
                averageSessionDurationMinutes = 0,
                longestSessionMinutes = 0,
                totalPausedMinutes = 0,
                totalDistractionMinutes = 0,
                appSwitchCount = 0,
                goalCompletionPercentage = 0
            )
        }

        val totalFocusedSecs = daysSessions.sumOf { it.focusedDurationSeconds }
        val totalPausedSecs = daysSessions.sumOf { it.pausedDurationSeconds }
        val totalDistractionSecs = daysSessions.sumOf { it.distractedDurationSeconds }
        val totalAppSwitches = daysSessions.sumOf { it.appSwitchCount }
        val longestSecs = daysSessions.maxOfOrNull { it.focusedDurationSeconds } ?: 0L

        val totalFocusedMins = (totalFocusedSecs / 60).toInt()
        val avgDurationMins = if (daysSessions.isNotEmpty()) (totalFocusedMins / daysSessions.size) else 0
        val longestMins = (longestSecs / 60).toInt()
        val pausedMins = (totalPausedSecs / 60).toInt()
        val distractionMins = (totalDistractionSecs / 60).toInt()

        val goalCompletion = if (dailyGoalMinutes > 0) {
            ((totalFocusedMins.toDouble() / dailyGoalMinutes.toDouble()) * 100).toInt().coerceAtMost(100)
        } else 100

        return DailyFocusStats(
            date = targetDate,
            totalFocusedMinutes = totalFocusedMins,
            sessionCount = daysSessions.size,
            averageSessionDurationMinutes = avgDurationMins,
            longestSessionMinutes = longestMins,
            totalPausedMinutes = pausedMins,
            totalDistractionMinutes = distractionMins,
            appSwitchCount = totalAppSwitches,
            goalCompletionPercentage = goalCompletion
        )
    }

    fun computeWeeklyStats(
        sessions: List<FocusSessionEntity>,
        weekStartDate: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    ): WeeklyFocusStats {
        val weekEndDate = weekStartDate.plusDays(6)
        val today = LocalDate.now()

        val weekSessions = sessions.filter {
            val d = Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate()
            !d.isBefore(weekStartDate) && !d.isAfter(weekEndDate)
        }

        val dailyBars = mutableListOf<WeeklyDayBar>()
        var totalSecs = 0L
        var activeDays = 0
        var bestDay = "N/A"
        var bestDaySecs = 0L

        for (i in 0..6) {
            val date = weekStartDate.plusDays(i.toLong())
            val daySessions = weekSessions.filter {
                Instant.ofEpochMilli(it.startTimestamp).atZone(zoneId).toLocalDate() == date
            }
            val daySecs = daySessions.sumOf { it.focusedDurationSeconds }
            totalSecs += daySecs

            if (daySecs > 0) activeDays++
            if (daySecs > bestDaySecs) {
                bestDaySecs = daySecs
                bestDay = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }

            dailyBars.add(
                WeeklyDayBar(
                    dayOfWeek = date.dayOfWeek,
                    dayName = date.dayOfWeek.name.take(3),
                    focusHours = (daySecs / 3600f),
                    isToday = date == today
                )
            )
        }

        val totalHours = totalSecs / 3600f
        val avgDailyMins = if (dailyBars.isNotEmpty()) ((totalSecs / 60) / 7).toInt() else 0
        val consistency = ((activeDays.toDouble() / 7.0) * 100).toInt()

        return WeeklyFocusStats(
            totalWeeklyHours = totalHours,
            dailyBreakdown = dailyBars,
            averageDailyMinutes = avgDailyMins,
            bestDayName = if (bestDaySecs > 0) bestDay else "None",
            bestDayHours = bestDaySecs / 3600f,
            consistencyPercentage = consistency
        )
    }

    fun computeAppStats(usages: List<FocusAppUsageEntity>): List<AppProductivityStat> {
        return usages
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                val totalDuration = list.sumOf { it.durationSeconds }
                val first = list.first()
                val isDistraction = first.classification == AppClassification.DISTRACTING_APP
                AppProductivityStat(
                    packageName = pkg,
                    applicationName = first.applicationName,
                    totalDurationSeconds = totalDuration,
                    visitCount = list.size,
                    classification = first.classification,
                    isDistraction = isDistraction
                )
            }
            .sortedByDescending { it.totalDurationSeconds }
    }
}
