package com.naman.focusos.domain.analytics

import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.data.model.sortedChronologically
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DayStats(
    val date: LocalDate,
    val dayName: String,
    val totalTasks: Int,
    val completedOnTime: Int,
    val completedLate: Int,
    val missed: Int,
    val pending: Int,
    val skipped: Int,
    val plannedDurationMinutes: Int,
    val completionRate: Float,
    val tasks: List<TaskEntity> = emptyList()
)

data class WeeklyAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<DayStats>,
    val totalCompletedOnTime: Int,
    val totalCompletedLate: Int,
    val totalMissed: Int,
    val totalPending: Int,
    val totalSkipped: Int,
    val overallCompletionRate: Float,
    val bestDay: String?,
    val weakestDay: String?
)

data class MonthlySummary(
    val monthName: String,
    val monthNumber: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val lateTasks: Int,
    val missedTasks: Int,
    val completionRate: Float
)

data class MonthlyAnalytics(
    val yearMonth: YearMonth,
    val totalTasks: Int,
    val completedTasks: Int,
    val lateTasks: Int,
    val missedTasks: Int,
    val monthlyCompletionRate: Float,
    val dailyHeatmap: Map<Int, Float>,        // Day of month 1..31 -> completion rate 0..100
    val habitConsistencyRate: Float
)

data class YearlyAnalytics(
    val year: Int,
    val monthlySummaries: List<MonthlySummary>,
    val totalTasks: Int,
    val totalCompleted: Int,
    val totalLate: Int,
    val totalMissed: Int,
    val yearlyCompletionRate: Float,
    val habitConsistencyRate: Float
)

object ProductivityIntelligenceEngine {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Compute comprehensive DayStats for a single date given tasks.
     */
    fun calculateDayStats(
        date: LocalDate,
        tasks: List<TaskEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): DayStats {
        var onTime = 0
        var late = 0
        var missed = 0
        var pending = 0
        var skipped = 0
        var plannedMinutes = 0

        for (task in tasks) {
            plannedMinutes += task.durationMinutes
            val effectiveStatus = task.evaluateEffectiveStatus(now)

            when (effectiveStatus) {
                TaskStatus.COMPLETED_ON_TIME -> onTime++
                TaskStatus.COMPLETED_LATE -> late++
                TaskStatus.MISSED -> missed++
                TaskStatus.UPCOMING, TaskStatus.IN_PROGRESS -> pending++
                TaskStatus.SKIPPED -> skipped++
            }
        }

        val eligibleTasks = tasks.size - skipped
        val totalCompleted = onTime + late
        val rate = if (eligibleTasks > 0) (totalCompleted.toFloat() / eligibleTasks.toFloat()) * 100f else 0f

        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val sortedTasks = tasks.sortedChronologically()

        return DayStats(
            date = date,
            dayName = dayName,
            totalTasks = tasks.size,
            completedOnTime = onTime,
            completedLate = late,
            missed = missed,
            pending = pending,
            skipped = skipped,
            plannedDurationMinutes = plannedMinutes,
            completionRate = rate,
            tasks = sortedTasks
        )
    }

    /**
     * Compute 7-day Weekly Analytics (e.g. Sunday to Saturday or Monday to Sunday).
     */
    fun calculateWeeklyAnalytics(
        weekStartDate: LocalDate,
        tasksByDate: Map<LocalDate, List<TaskEntity>>,
        now: LocalDateTime = LocalDateTime.now()
    ): WeeklyAnalytics {
        val days = mutableListOf<DayStats>()
        var totalOnTime = 0
        var totalLate = 0
        var totalMissed = 0
        var totalPending = 0
        var totalSkipped = 0

        for (i in 0 until 7) {
            val date = weekStartDate.plusDays(i.toLong())
            val tasks = tasksByDate[date] ?: emptyList()
            val stats = calculateDayStats(date, tasks, now)
            days.add(stats)

            totalOnTime += stats.completedOnTime
            totalLate += stats.completedLate
            totalMissed += stats.missed
            totalPending += stats.pending
            totalSkipped += stats.skipped
        }

        val totalCompleted = totalOnTime + totalLate
        val totalEligible = (totalCompleted + totalMissed + totalPending)
        val overallRate = if (totalEligible > 0) (totalCompleted.toFloat() / totalEligible.toFloat()) * 100f else 0f

        // Determine best and weakest day among days that had tasks
        val daysWithTasks = days.filter { it.totalTasks - it.skipped > 0 }
        val bestDay = daysWithTasks.maxByOrNull { it.completionRate }?.dayName
        val weakestDay = daysWithTasks.minByOrNull { it.completionRate }?.dayName

        return WeeklyAnalytics(
            startDate = weekStartDate,
            endDate = weekStartDate.plusDays(6),
            days = days,
            totalCompletedOnTime = totalOnTime,
            totalCompletedLate = totalLate,
            totalMissed = totalMissed,
            totalPending = totalPending,
            totalSkipped = totalSkipped,
            overallCompletionRate = overallRate,
            bestDay = bestDay,
            weakestDay = weakestDay
        )
    }

    /**
     * Compute Monthly Analytics with daily heatmap and habit consistency.
     */
    fun calculateMonthlyAnalytics(
        yearMonth: YearMonth,
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): MonthlyAnalytics {
        val daysInMonth = yearMonth.lengthOfMonth()
        val tasksByDay = tasks.groupBy {
            try { LocalDate.parse(it.scheduledDate, dateFormatter).dayOfMonth } catch (e: Exception) { 0 }
        }

        val heatmap = mutableMapOf<Int, Float>()
        var totalOnTime = 0
        var totalLate = 0
        var totalMissed = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dayTasks = tasksByDay[day] ?: emptyList()
            val stats = calculateDayStats(date, dayTasks, now)
            heatmap[day] = stats.completionRate

            totalOnTime += stats.completedOnTime
            totalLate += stats.completedLate
            totalMissed += stats.missed
        }

        val totalCompleted = totalOnTime + totalLate
        val totalRecorded = totalCompleted + totalMissed
        val monthlyRate = if (totalRecorded > 0) (totalCompleted.toFloat() / totalRecorded.toFloat()) * 100f else 0f

        // Habit consistency
        val activeHabitCount = habits.filter { it.isActive }.size
        val totalPossibleHabitEntries = activeHabitCount * daysInMonth
        val completedHabits = completions.count { it.isCompleted }
        val habitRate = if (totalPossibleHabitEntries > 0) {
            ((completedHabits.toFloat() / totalPossibleHabitEntries.toFloat()) * 100f).coerceAtMost(100f)
        } else 0f

        return MonthlyAnalytics(
            yearMonth = yearMonth,
            totalTasks = tasks.size,
            completedTasks = totalCompleted,
            lateTasks = totalLate,
            missedTasks = totalMissed,
            monthlyCompletionRate = monthlyRate,
            dailyHeatmap = heatmap,
            habitConsistencyRate = habitRate
        )
    }

    /**
     * Compute Yearly Analytics.
     */
    fun calculateYearlyAnalytics(
        year: Int,
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): YearlyAnalytics {
        val monthlySummaries = mutableListOf<MonthlySummary>()
        var yearTotalTasks = 0
        var yearCompleted = 0
        var yearLate = 0
        var yearMissed = 0

        val tasksByMonth = tasks.groupBy {
            try { LocalDate.parse(it.scheduledDate, dateFormatter).monthValue } catch (e: Exception) { 0 }
        }

        for (month in 1..12) {
            val ym = YearMonth.of(year, month)
            val mTasks = tasksByMonth[month] ?: emptyList()
            var mOnTime = 0
            var mLate = 0
            var mMissed = 0

            for (t in mTasks) {
                when (t.evaluateEffectiveStatus(now)) {
                    TaskStatus.COMPLETED_ON_TIME -> mOnTime++
                    TaskStatus.COMPLETED_LATE -> mLate++
                    TaskStatus.MISSED -> mMissed++
                    else -> {}
                }
            }

            val mCompleted = mOnTime + mLate
            val mEligible = mCompleted + mMissed
            val rate = if (mEligible > 0) (mCompleted.toFloat() / mEligible.toFloat()) * 100f else 0f

            val monthName = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            monthlySummaries.add(
                MonthlySummary(
                    monthName = monthName,
                    monthNumber = month,
                    totalTasks = mTasks.size,
                    completedTasks = mCompleted,
                    lateTasks = mLate,
                    missedTasks = mMissed,
                    completionRate = rate
                )
            )

            yearTotalTasks += mTasks.size
            yearCompleted += mCompleted
            yearLate += mLate
            yearMissed += mMissed
        }

        val totalEvaluated = yearCompleted + yearMissed
        val yearlyRate = if (totalEvaluated > 0) (yearCompleted.toFloat() / totalEvaluated.toFloat()) * 100f else 0f

        val activeHabits = habits.count { it.isActive }
        val habitPossibilities = activeHabits * 365
        val habitRate = if (habitPossibilities > 0) {
            ((completions.count { it.isCompleted }.toFloat() / habitPossibilities.toFloat()) * 100f).coerceAtMost(100f)
        } else 0f

        return YearlyAnalytics(
            year = year,
            monthlySummaries = monthlySummaries,
            totalTasks = yearTotalTasks,
            totalCompleted = yearCompleted,
            totalLate = yearLate,
            totalMissed = yearMissed,
            yearlyCompletionRate = yearlyRate,
            habitConsistencyRate = habitRate
        )
    }

    /**
     * Calculate current and best streak for a habit
     */
    fun calculateHabitStreak(
        completions: List<HabitCompletionEntity>,
        today: LocalDate = LocalDate.now()
    ): Pair<Int, Int> { // Pair(currentStreak, bestStreak)
        if (completions.isEmpty()) return Pair(0, 0)

        val completedDates = completions
            .filter { it.isCompleted }
            .mapNotNull {
                try { LocalDate.parse(it.date, dateFormatter) } catch (e: Exception) { null }
            }
            .toSet()

        if (completedDates.isEmpty()) return Pair(0, 0)

        // Calculate current streak backwards from today (or yesterday if today not yet checked)
        var currentStreak = 0
        var checkDate = today
        if (!completedDates.contains(checkDate)) {
            checkDate = today.minusDays(1)
        }

        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Calculate best streak across all history
        val sortedDates = completedDates.sorted()
        var bestStreak = 0
        var runningStreak = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate == null || d == prevDate.plusDays(1)) {
                runningStreak++
            } else {
                runningStreak = 1
            }
            if (runningStreak > bestStreak) {
                bestStreak = runningStreak
            }
            prevDate = d
        }

        return Pair(currentStreak, bestStreak)
    }
}
