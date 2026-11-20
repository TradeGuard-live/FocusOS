package com.naman.focusos.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.domain.analytics.MonthlyAnalytics
import com.naman.focusos.domain.analytics.YearlyAnalytics
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.StatColumn
import com.naman.focusos.ui.components.WeeklyProductivityChart
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import com.naman.focusos.ui.theme.WarmAmber
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "ANALYTICS & INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Performance Insights",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                }
            }

            // Period Selector Tabs (Weekly, Monthly, Yearly)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .padding(4.dp)
                ) {
                    AnalyticsPeriod.entries.forEach { period ->
                        val isSelected = period == uiState.selectedPeriod
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) FocusTheme.colors.primaryAccent else Color.Transparent)
                                .clickable { viewModel.setPeriod(period) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = period.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (FocusTheme.colors.isDark) Color.Black else Color.White) else FocusTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Period Content
            when (uiState.selectedPeriod) {
                AnalyticsPeriod.WEEKLY -> {
                    uiState.weeklyAnalytics?.let { weekly ->
                        item {
                            WeeklyProductivityChart(
                                analytics = weekly,
                                onPreviousWeek = { viewModel.onPreviousWeek() },
                                onNextWeek = { viewModel.onNextWeek() }
                            )
                        }

                        item {
                            Text(
                                text = "WEEKLY METRICS BREAKDOWN",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                            )
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricCard(
                                    title = "Completed On-Time",
                                    count = weekly.totalCompletedOnTime,
                                    color = MintEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Completed Late",
                                    count = weekly.totalCompletedLate,
                                    color = WarmAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricCard(
                                    title = "Missed Tasks",
                                    count = weekly.totalMissed,
                                    color = CrimsonCoral,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Pending / Upcoming",
                                    count = weekly.totalPending,
                                    color = ElectricBlue,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                AnalyticsPeriod.MONTHLY -> {
                    uiState.monthlyAnalytics?.let { monthly ->
                        item {
                            MonthlyAnalyticsSection(
                                monthly = monthly,
                                onPrev = { viewModel.onPreviousMonth() },
                                onNext = { viewModel.onNextMonth() }
                            )
                        }
                    }
                }

                AnalyticsPeriod.YEARLY -> {
                    uiState.yearlyAnalytics?.let { yearly ->
                        item {
                            YearlyAnalyticsSection(
                                yearly = yearly,
                                onPrev = { viewModel.onPreviousYear() },
                                onNext = { viewModel.onNextYear() }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun MonthlyAnalyticsSection(
    monthly: MonthlyAnalytics,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    FocusCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${monthly.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${monthly.yearMonth.year}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.textPrimary)
                )

                Row {
                    IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = FocusTheme.colors.textSecondary)
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = FocusTheme.colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatColumn(label = "Monthly Completion", value = "${monthly.monthlyCompletionRate.toInt()}%", valueColor = FocusTheme.colors.success)
                StatColumn(label = "Completed Tasks", value = "${monthly.completedTasks}", valueColor = FocusTheme.colors.primaryAccent)
                StatColumn(label = "Missed Tasks", value = "${monthly.missedTasks}", valueColor = FocusTheme.colors.danger)
                StatColumn(label = "Habit Consistency", value = "${monthly.habitConsistencyRate.toInt()}%", valueColor = Color(0xFF8B5CF6))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "DAILY ACTIVITY HEATMAP",
                style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 31-day heatmap grid (rows of 7 days)
            val daysInMonth = monthly.yearMonth.lengthOfMonth()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until (daysInMonth + 6) / 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val day = r * 7 + c + 1
                            if (day <= daysInMonth) {
                                val rate = monthly.dailyHeatmap[day] ?: 0f
                                val heatColor = when {
                                    rate >= 80f -> FocusTheme.colors.success
                                    rate >= 50f -> FocusTheme.colors.primaryAccent
                                    rate > 0f -> FocusTheme.colors.warning
                                    else -> FocusTheme.colors.surfaceVariant
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(heatColor.copy(alpha = if (rate > 0f) 0.85f else 0.4f))
                                ) {
                                    Text(
                                        text = "$day",
                                        color = if (rate > 0f) (if (FocusTheme.colors.isDark) Color.Black else Color.White) else FocusTheme.colors.textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyAnalyticsSection(
    yearly: YearlyAnalytics,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    FocusCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "YEAR ${yearly.year}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.textPrimary)
                )

                Row {
                    IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = FocusTheme.colors.textSecondary)
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = FocusTheme.colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatColumn(label = "Yearly Rate", value = "${yearly.yearlyCompletionRate.toInt()}%", valueColor = FocusTheme.colors.success)
                StatColumn(label = "Total Completed", value = "${yearly.totalCompleted}", valueColor = FocusTheme.colors.primaryAccent)
                StatColumn(label = "Total Late", value = "${yearly.totalLate}", valueColor = FocusTheme.colors.warning)
                StatColumn(label = "Total Missed", value = "${yearly.totalMissed}", valueColor = FocusTheme.colors.danger)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "12-MONTH COMPLETION COMPARISON",
                style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 12 Monthly Bars
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                yearly.monthlySummaries.forEach { m ->
                    val ratio = (m.completionRate / 100f).coerceIn(0.08f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (m.completionRate > 0) FocusTheme.colors.success else FocusTheme.colors.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = m.monthName.take(1),
                            style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary, fontSize = 9.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    FocusCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary, fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 24.sp
                )
            )
        }
    }
}
