package com.naman.focusos.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.domain.focus.AnalyticsDateFilter
import com.naman.focusos.domain.focus.BestFocusPeriodRecommendation
import com.naman.focusos.domain.focus.DistractionDashboardData
import com.naman.focusos.domain.focus.DistractionPatternReport
import com.naman.focusos.domain.focus.FocusScoreDetail
import com.naman.focusos.domain.focus.HeatmapCell
import com.naman.focusos.domain.focus.HourlyProductivity
import com.naman.focusos.domain.focus.InsightType
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.domain.focus.ProductivityInsight
import com.naman.focusos.domain.focus.RecommendationConfidence
import com.naman.focusos.domain.focus.ScoreCategory
import com.naman.focusos.domain.focus.TaskEfficiencySummary
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.theme.FocusTheme
import java.time.DayOfWeek

@Composable
fun FocusAnalyticsView(
    uiState: FocusUiState,
    onFilterSelected: (AnalyticsDateFilter) -> Unit,
    onSelectHeatmapCell: (HeatmapCell?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Bar
        AnalyticsFilterBar(
            selectedFilter = uiState.analyticsDateFilter,
            onFilterSelected = onFilterSelected
        )

        // SECTION 1: Focus Score Dashboard & Contributor Breakdown
        FocusScoreCard(scoreDetail = uiState.focusScore)

        // SECTION 2: Best Focus Hours & Interactive 7x19 Heatmap
        PeakFocusHoursCard(
            bestPeriod = uiState.bestFocusPeriod,
            hourlyData = uiState.hourlyProductivity
        )

        WeeklyHeatmapCard(
            cells = uiState.weeklyHeatmap,
            selectedCell = uiState.selectedHeatmapCell,
            onSelectCell = onSelectHeatmapCell
        )

        // SECTION 3: Distraction Intelligence Dashboard
        DistractionIntelligenceCard(
            distractionData = uiState.distractionDashboard,
            patterns = uiState.distractionPatterns
        )

        // SECTION 4: Task vs Focus Comparison & Category Analytics
        TaskFocusComparisonCard(taskEfficiency = uiState.taskEfficiency)

        // SECTION 5: Productivity Insights Engine
        ProductivityInsightsList(insights = uiState.productivityInsights)

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// =============================================================================
// DATE FILTER BAR
// =============================================================================

@Composable
private fun AnalyticsFilterBar(
    selectedFilter: AnalyticsDateFilter,
    onFilterSelected: (AnalyticsDateFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsDateFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) FocusTheme.colors.primaryAccent.copy(alpha = 0.16f)
                        else FocusTheme.colors.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) FocusTheme.colors.primaryAccent else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// =============================================================================
// SECTION 1: INTELLIGENT FOCUS SCORE DASHBOARD
// =============================================================================

@Composable
private fun FocusScoreCard(scoreDetail: FocusScoreDetail) {
    FocusCard {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "INTELLIGENT FOCUS SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                // Trend badge
                val trend = scoreDetail.trendPercentage
                val isPositive = trend != null && trend > 0f
                val isNegative = trend != null && trend < 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isPositive -> FocusTheme.colors.success.copy(alpha = 0.12f)
                                isNegative -> FocusTheme.colors.error.copy(alpha = 0.12f)
                                else -> FocusTheme.colors.surfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val icon = when {
                        isPositive -> Icons.AutoMirrored.Filled.TrendingUp
                        isNegative -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    }
                    val trendColor = when {
                        isPositive -> FocusTheme.colors.success
                        isNegative -> FocusTheme.colors.error
                        else -> FocusTheme.colors.textSecondary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (trend != null && trend != 0f) "${if (trend > 0) "+" else ""}${trend.toInt()}%" else "Stable",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = trendColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Circular Gauge and Score Callout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Score Gauge Arc
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scoreFraction = (scoreDetail.overallScore.toFloat() / 100f).coerceIn(0f, 1f)
                    val arcColor = when (scoreDetail.category) {
                        ScoreCategory.OUTSTANDING, ScoreCategory.EXCELLENT -> FocusTheme.colors.success
                        ScoreCategory.GOOD -> FocusTheme.colors.primaryAccent
                        ScoreCategory.NEEDS_IMPROVEMENT -> FocusTheme.colors.warning
                        ScoreCategory.LOW_CONSISTENCY -> FocusTheme.colors.error
                        ScoreCategory.INSUFFICIENT_DATA -> FocusTheme.colors.textMuted
                    }
                    val trackColor = FocusTheme.colors.surfaceVariant

                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        // Track
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Progress
                        if (scoreFraction > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = 135f,
                                sweepAngle = 270f * scoreFraction,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (scoreDetail.category == ScoreCategory.INSUFFICIENT_DATA) "--" else "${scoreDetail.overallScore}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FocusTheme.colors.textPrimary,
                                fontSize = 34.sp
                            )
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Category & Meta stats
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (scoreDetail.category) {
                        ScoreCategory.OUTSTANDING, ScoreCategory.EXCELLENT -> FocusTheme.colors.success
                        ScoreCategory.GOOD -> FocusTheme.colors.primaryAccent
                        ScoreCategory.NEEDS_IMPROVEMENT -> FocusTheme.colors.warning
                        ScoreCategory.LOW_CONSISTENCY -> FocusTheme.colors.error
                        ScoreCategory.INSUFFICIENT_DATA -> FocusTheme.colors.textMuted
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = scoreDetail.category.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Text(
                        text = scoreDetail.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${scoreDetail.sessionsIncludedCount} sessions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary)
                        )
                        Text(
                            text = "${scoreDetail.coveragePercentage}% coverage",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Contributor Components
            Text(
                text = "SCORE CONTRIBUTORS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreContributorBar(
                    label = "Focus Completion (40%)",
                    points = (scoreDetail.focusCompletionScore * 0.40f).toInt(),
                    maxPoints = 40,
                    percent = scoreDetail.focusCompletionScore.toInt(),
                    color = FocusTheme.colors.primaryAccent
                )
                ScoreContributorBar(
                    label = "Distraction Control (30%)",
                    points = (scoreDetail.distractionControlScore * 0.30f).toInt(),
                    maxPoints = 30,
                    percent = scoreDetail.distractionControlScore.toInt(),
                    color = FocusTheme.colors.success
                )
                ScoreContributorBar(
                    label = "Consistency & Cadence (20%)",
                    points = (scoreDetail.consistencyScore * 0.20f).toInt(),
                    maxPoints = 20,
                    percent = scoreDetail.consistencyScore.toInt(),
                    color = FocusTheme.colors.info
                )
                ScoreContributorBar(
                    label = "Target Completion (10%)",
                    points = (scoreDetail.sessionCompletionScore * 0.10f).toInt(),
                    maxPoints = 10,
                    percent = scoreDetail.sessionCompletionScore.toInt(),
                    color = FocusTheme.colors.warning
                )
            }

            // Positive & Negative Contributors List
            if (scoreDetail.positiveContributors.isNotEmpty() || scoreDetail.negativeContributors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    scoreDetail.positiveContributors.take(2).forEach { factor ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FocusTheme.colors.success,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = factor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    scoreDetail.negativeContributors.take(2).forEach { factor ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = FocusTheme.colors.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = factor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreContributorBar(
    label: String,
    points: Int,
    maxPoints: Int,
    percent: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
            Text(
                text = "$points / $maxPoints pts (${percent}%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(FocusTheme.colors.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent.toFloat() / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// =============================================================================
// SECTION 2: BEST FOCUS HOURS & WEEKLY HEATMAP
// =============================================================================

@Composable
private fun PeakFocusHoursCard(
    bestPeriod: BestFocusPeriodRecommendation,
    hourlyData: List<HourlyProductivity>
) {
    FocusCard {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "BEST FOCUS HOURS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                val confidenceColor = when (bestPeriod.confidence) {
                    RecommendationConfidence.HIGH -> FocusTheme.colors.success
                    RecommendationConfidence.MODERATE -> FocusTheme.colors.warning
                    RecommendationConfidence.LOW -> FocusTheme.colors.textMuted
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(confidenceColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${bestPeriod.confidence.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = confidenceColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recommended Window Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocusTheme.colors.surfaceVariant)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bestPeriod.bestTwoHourFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FocusTheme.colors.primaryAccent
                            )
                        )
                        Text(
                            text = "${bestPeriod.percentageOfWorkInBestPeriod}% of total focus",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bestPeriod.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "💡 Best Day: ${bestPeriod.bestDayOfWeek} • Peak Hour: ${bestPeriod.bestHourFormatted}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 24-Hour Productivity Bar Chart
            Text(
                text = "HOURLY FOCUS DISTRIBUTION (24 HOURS)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val maxFocusedMinutes = (hourlyData.maxOfOrNull { it.totalFocusedMinutes } ?: 1).coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                hourlyData.forEach { hour ->
                    val fraction = (hour.totalFocusedMinutes.toFloat() / maxFocusedMinutes.toFloat()).coerceIn(0.06f, 1f)
                    val isPeak = hour.hour >= bestPeriod.startHour && hour.hour < bestPeriod.endHour && hour.totalFocusedMinutes > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.width(18.dp)
                    ) {
                        if (hour.totalFocusedMinutes > 0) {
                            Text(
                                text = "${hour.totalFocusedMinutes}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isPeak) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isPeak) FocusTheme.colors.primaryAccent
                                    else if (hour.totalFocusedMinutes > 0) FocusTheme.colors.primaryAccent.copy(alpha = 0.45f)
                                    else FocusTheme.colors.surfaceVariant
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${hour.hour}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isPeak) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyHeatmapCard(
    cells: List<HeatmapCell>,
    selectedCell: HeatmapCell?,
    onSelectCell: (HeatmapCell?) -> Unit
) {
    FocusCard {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "WEEKLY HEATMAP (5 AM – 11 PM)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                Text(
                    text = "Tap cell to inspect",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heatmap Matrix: 7 Columns (Mon..Sun) and 19 rows (5..23)
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            // Days Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(32.dp)) // Hour label space
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hourly Rows (5..23)
            (5..23).forEach { hour ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour label
                    Text(
                        text = String.format("%02d", hour),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.width(32.dp)
                    )

                    // 7 Days
                    (1..7).forEach { dayVal ->
                        val dayOfWeek = DayOfWeek.of(dayVal)
                        val cell = cells.find { it.dayOfWeek == dayOfWeek && it.hour == hour }
                        val focusedMinutes = cell?.focusedMinutes ?: 0
                        val isSelected = selectedCell?.dayOfWeek == dayOfWeek && selectedCell.hour == hour

                        val cellColor = when {
                            focusedMinutes == 0 -> FocusTheme.colors.surfaceVariant.copy(alpha = 0.5f)
                            focusedMinutes <= 15 -> FocusTheme.colors.primaryAccent.copy(alpha = 0.25f)
                            focusedMinutes <= 30 -> FocusTheme.colors.primaryAccent.copy(alpha = 0.50f)
                            focusedMinutes <= 45 -> FocusTheme.colors.primaryAccent.copy(alpha = 0.75f)
                            else -> FocusTheme.colors.primaryAccent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .padding(horizontal = 1.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellColor)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) FocusTheme.colors.textPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .clickable {
                                    if (isSelected) onSelectCell(null)
                                    else onSelectCell(cell)
                                }
                        )
                    }
                }
            }

            // Cell Inspector Card if selected
            if (selectedCell != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedCell.dayName} at ${String.format("%02d:00", selectedCell.hour)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.primaryAccent
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedCell.sessionCount} sessions • ${selectedCell.distractionMinutes}m distraction",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Text(
                            text = "${selectedCell.focusedMinutes} min focused",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FocusTheme.colors.textPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SECTION 3: DISTRACTION INTELLIGENCE DASHBOARD
// =============================================================================

@Composable
private fun DistractionIntelligenceCard(
    distractionData: DistractionDashboardData,
    patterns: DistractionPatternReport
) {
    FocusCard {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DISTRACTION INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                if (distractionData.periodChangePercentage != null) {
                    val isImprovement = distractionData.isImprovement
                    val changeColor = if (isImprovement) FocusTheme.colors.success else FocusTheme.colors.error
                    Text(
                        text = "${if (distractionData.periodChangePercentage!! > 0) "+" else ""}${distractionData.periodChangePercentage!!.toInt()}% vs prev",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6-metric Dashboard Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsStatTile(
                    label = "Total Distraction",
                    value = LiveTimerSnapshot.formatDuration(distractionData.totalDistractionSeconds),
                    color = if (distractionData.totalDistractionSeconds > 0) FocusTheme.colors.error else FocusTheme.colors.success,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "Incidents",
                    value = "${distractionData.totalDistractionsCount}",
                    color = FocusTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "App Switches",
                    value = "${distractionData.totalAppSwitches}",
                    color = FocusTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsStatTile(
                    label = "Avg Incident",
                    value = "${distractionData.averageDistractionSeconds}s",
                    color = FocusTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "Longest Visit",
                    value = LiveTimerSnapshot.formatDuration(distractionData.longestDistractionSeconds),
                    color = FocusTheme.colors.warning,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "Session Share",
                    value = "${distractionData.distractionPercentage.toInt()}%",
                    color = if (distractionData.distractionPercentage > 10f) FocusTheme.colors.error else FocusTheme.colors.success,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ranked Applications Breakdown
            Text(
                text = "APPLICATION USAGE CLASSIFICATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (distractionData.rankedApps.isEmpty()) {
                Text(
                    text = "No recorded app usage in this time frame.",
                    style = MaterialTheme.typography.bodySmall.copy(color = FocusTheme.colors.textSecondary),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                val maxAppDuration = (distractionData.rankedApps.maxOfOrNull { it.totalDurationSeconds } ?: 1L).coerceAtLeast(1L)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    distractionData.rankedApps.take(6).forEach { app ->
                        val barFraction = (app.totalDurationSeconds.toFloat() / maxAppDuration.toFloat()).coerceIn(0.04f, 1f)
                        val badgeColor = when (app.classification) {
                            AppClassification.DISTRACTING_APP -> FocusTheme.colors.error
                            AppClassification.PRODUCTIVE_APP -> FocusTheme.colors.success
                            AppClassification.PAUSED_SESSION_USAGE -> FocusTheme.colors.warning
                            else -> FocusTheme.colors.textSecondary
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.applicationName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FocusTheme.colors.textPrimary
                                    )
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = LiveTimerSnapshot.formatDuration(app.totalDurationSeconds),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusTheme.colors.textSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = app.classification.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(FocusTheme.colors.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(badgeColor)
                                )
                            }
                        }
                    }
                }
            }

            // Distraction Patterns Summary
            if (patterns.mostDistractingHourFormatted != "N/A") {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PATTERN OBSERVATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.warning,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = "Most distractions cluster around ${patterns.mostDistractingHourFormatted} (${patterns.mostDistractingDay}) averaging ${patterns.distractionsPerSession} distractions per session.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "Observation: ${patterns.observationSummary}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SECTION 4: TASK VS FOCUS COMPARISON & CATEGORY ANALYTICS
// =============================================================================

@Composable
private fun TaskFocusComparisonCard(taskEfficiency: TaskEfficiencySummary) {
    FocusCard {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "TASK & FOCUS EFFICIENCY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                Text(
                    text = "${taskEfficiency.averageEstimationAccuracy}% Accuracy",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Symmetric accuracy & planned vs actual overview
            val plannedHours = taskEfficiency.overallEstimatedMinutes / 60f
            val focusedHours = taskEfficiency.overallActualFocusedMinutes / 60f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsStatTile(
                    label = "Planned Hours",
                    value = "${String.format("%.1f", plannedHours)}h",
                    color = FocusTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "Focused Hours",
                    value = "${String.format("%.1f", focusedHours)}h",
                    color = FocusTheme.colors.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatTile(
                    label = "Accuracy",
                    value = "${taskEfficiency.averageEstimationAccuracy}%",
                    color = FocusTheme.colors.success,
                    modifier = Modifier.weight(1f)
                )
            }

            // Category Focus Breakdown
            if (taskEfficiency.categoryBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "CATEGORY FOCUS DISTRIBUTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    taskEfficiency.categoryBreakdown.forEach { cat ->
                        val catHours = cat.totalFocusedMinutes / 60f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.categoryName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FocusTheme.colors.textPrimary
                                    )
                                )
                                Text(
                                    text = "${String.format("%.1f", catHours)}h (${cat.percentageOfTotalFocus}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FocusTheme.colors.primaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(FocusTheme.colors.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((cat.percentageOfTotalFocus.toFloat() / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(FocusTheme.colors.primaryAccent)
                                )
                            }
                        }
                    }
                }
            }

            // Linked Tasks comparison list
            if (taskEfficiency.taskComparisons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "LINKED TASKS VS ACTUAL TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    taskEfficiency.taskComparisons.take(5).forEach { taskItem ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FocusTheme.colors.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = taskItem.taskTitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FocusTheme.colors.textPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Plan: ${taskItem.estimatedMinutes}m • Focused: ${taskItem.actualFocusedMinutes}m",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusTheme.colors.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(FocusTheme.colors.surface)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${taskItem.estimationAccuracyPercentage}% Acc",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusTheme.colors.primaryAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SECTION 5: PRODUCTIVITY INSIGHTS LIST
// =============================================================================

@Composable
private fun ProductivityInsightsList(insights: List<ProductivityInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = FocusTheme.colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "PRODUCTIVITY INTELLIGENCE INSIGHTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
        }

        insights.forEach { insight ->
            val icon = when (insight.type) {
                InsightType.POSITIVE_TREND -> Icons.Default.CheckCircle
                InsightType.OPPORTUNITY -> Icons.Default.Lightbulb
                InsightType.SCHEDULING_SUGGESTION -> Icons.Default.Timeline
                InsightType.TASK_EFFICIENCY -> Icons.Default.Star
            }

            val accentColor = when (insight.type) {
                InsightType.POSITIVE_TREND -> FocusTheme.colors.success
                InsightType.OPPORTUNITY -> FocusTheme.colors.warning
                InsightType.SCHEDULING_SUGGESTION -> FocusTheme.colors.primaryAccent
                InsightType.TASK_EFFICIENCY -> Color(0xFFFFB300)
            }

            FocusCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.textPrimary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = insight.type.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            lineHeight = 16.sp,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FocusTheme.colors.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Evidence: ${insight.evidence}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE STAT TILE
// =============================================================================

@Composable
private fun AnalyticsStatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FocusTheme.colors.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
