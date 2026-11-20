package com.naman.focusos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.domain.analytics.DayStats
import com.naman.focusos.domain.analytics.WeeklyAnalytics
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import com.naman.focusos.ui.theme.WarmAmber
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyProductivityChart(
    analytics: WeeklyAnalytics,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val maxTasks = (analytics.days.maxOfOrNull { it.totalTasks } ?: 1).coerceAtLeast(4)

    FocusCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Week Range & Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "WEEKLY PRODUCTIVITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val formatter = DateTimeFormatter.ofPattern("MMM dd")
                    Text(
                        text = "${analytics.startDate.format(formatter)} – ${analytics.endDate.format(formatter)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousWeek, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Week",
                            tint = FocusTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onNextWeek, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Week",
                            tint = FocusTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Legend
            ChartLegend()

            Spacer(modifier = Modifier.height(18.dp))

            // Seven Vertical Stacked Bars
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 4.dp)
            ) {
                analytics.days.forEachIndexed { index, day ->
                    val isSelected = selectedDayIndex == index
                    WeeklyBarColumn(
                        day = day,
                        maxTasks = maxTasks,
                        isSelected = isSelected,
                        onClick = {
                            selectedDayIndex = if (selectedDayIndex == index) null else index
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive Day Details Tooltip
            AnimatedVisibility(
                visible = selectedDayIndex != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedDayIndex?.let { idx ->
                    val day = analytics.days[idx]
                    DayDetailTooltip(day = day, is24Hour = is24Hour)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary stats footer: Best Day, Weakest Day, Completion Rate
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocusTheme.colors.surfaceVariant)
                    .padding(12.dp)
            ) {
                StatColumn(label = "Overall Rate", value = "${analytics.overallCompletionRate.toInt()}%", valueColor = FocusTheme.colors.success)
                StatColumn(label = "Best Day", value = analytics.bestDay ?: "–", valueColor = FocusTheme.colors.primaryAccent)
                StatColumn(label = "Weakest Day", value = analytics.weakestDay ?: "–", valueColor = FocusTheme.colors.warning)
            }
        }
    }
}

@Composable
fun WeeklyBarColumn(
    day: DayStats,
    maxTasks: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = day.totalTasks.toFloat()
    val animatedHeightRatio by animateFloatAsState(
        targetValue = if (total > 0) (total / maxTasks.toFloat()).coerceIn(0.1f, 1f) else 0.05f,
        animationSpec = tween(durationMillis = 600),
        label = "bar_height"
    )

    val barBg = if (isSelected) (if (FocusTheme.colors.isDark) Color(0xFF263352) else Color(0xFFBAE6FD)) else FocusTheme.colors.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clickable { onClick() }
    ) {
        // Stacked Bar Container
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .width(28.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(barBg)
                .then(if (isSelected) Modifier.border(1.5.dp, FocusTheme.colors.primaryAccent, RoundedCornerShape(8.dp)) else Modifier)
        ) {
            if (total > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedHeightRatio)
                ) {
                    // Pending / Remaining tasks segment (top)
                    if (day.pending > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(day.pending.toFloat())
                                .background(FocusTheme.colors.primaryAccent.copy(alpha = 0.7f))
                        )
                    }
                    // Missed tasks segment
                    if (day.missed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(day.missed.toFloat())
                                .background(FocusTheme.colors.danger)
                        )
                    }
                    // Completed Late segment
                    if (day.completedLate > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(day.completedLate.toFloat())
                                .background(FocusTheme.colors.warning)
                        )
                    }
                    // Completed on Time segment (bottom)
                    if (day.completedOnTime > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(day.completedOnTime.toFloat())
                                .background(FocusTheme.colors.success)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day label (e.g. Mon, Tue)
        Text(
            text = day.dayName.take(3),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun DayDetailTooltip(
    day: DayStats,
    is24Hour: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (FocusTheme.colors.isDark) Color(0xFF0F172A) else FocusTheme.colors.surfaceVariant)
            .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Date label, total count, completion rate badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val dateLabel = try {
                        day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                    } catch (e: Exception) {
                        day.dayName
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                    Text(
                        text = "${day.totalTasks} ${if (day.totalTasks == 1) "task" else "tasks"} recorded",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary
                        )
                    )
                }

                // Completion percentage badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FocusTheme.colors.primaryAccent.copy(alpha = if (FocusTheme.colors.isDark) 0.2f else 0.12f))
                        .border(1.dp, FocusTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${day.completionRate.toInt()}% Complete",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.primaryAccent,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown counters row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DayStatusBadge(
                    label = "On Time",
                    count = day.completedOnTime,
                    color = FocusTheme.colors.success,
                    modifier = Modifier.weight(1f)
                )
                DayStatusBadge(
                    label = "Late",
                    count = day.completedLate,
                    color = FocusTheme.colors.warning,
                    modifier = Modifier.weight(1f)
                )
                DayStatusBadge(
                    label = "Missed",
                    count = day.missed,
                    color = FocusTheme.colors.danger,
                    modifier = Modifier.weight(1f)
                )
                DayStatusBadge(
                    label = "Pending",
                    count = day.pending,
                    color = FocusTheme.colors.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FocusTheme.colors.border)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section Title: Tasks & Completion Times
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ALL TASKS & COMPLETION TIMES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                val completedCount = day.tasks.count { it.status.isCompleted() }
                Text(
                    text = "$completedCount/${day.tasks.size} Completed",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (day.tasks.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "No tasks recorded for this day",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocusTheme.colors.textMuted
                        )
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    day.tasks.forEach { task ->
                        DayTaskCompletionRow(task = task, is24Hour = is24Hour)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStatusBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (FocusTheme.colors.isDark) 0.15f else 0.10f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp, horizontal = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 13.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun DayTaskCompletionRow(
    task: TaskEntity,
    is24Hour: Boolean
) {
    val isCompleted = task.status.isCompleted()
    val completionTimeFormatted = task.getFormattedCompletionTime(is24Hour)
    val cardBg = if (FocusTheme.colors.isDark) Color(0xFF162032) else FocusTheme.colors.surface
    val borderCol = if (isCompleted) {
        if (task.status == TaskStatus.COMPLETED_LATE) FocusTheme.colors.warning.copy(alpha = 0.35f)
        else FocusTheme.colors.success.copy(alpha = 0.35f)
    } else {
        FocusTheme.colors.border
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Left column: Title & Schedule/Deadline info
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTheme.colors.textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val details = buildString {
                if (!task.startTime.isNullOrBlank()) {
                    append("Scheduled ${task.startTime}")
                }
                if (task.durationMinutes > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("${task.durationMinutes}m")
                }
                if (!task.deadlineTime.isNullOrBlank() && !isCompleted) {
                    if (isNotEmpty()) append(" • Due ")
                    append(task.deadlineTime)
                }
            }
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Right side: Exact completion time or current status
        if (isCompleted) {
            val isLate = task.status == TaskStatus.COMPLETED_LATE
            val badgeColor = if (isLate) FocusTheme.colors.warning else FocusTheme.colors.success
            val badgeText = if (completionTimeFormatted != null) {
                if (isLate) "Late at $completionTimeFormatted" else "Done at $completionTimeFormatted"
            } else {
                if (isLate) "Completed Late" else "Completed"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = if (FocusTheme.colors.isDark) 0.20f else 0.12f))
                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLate) "⏰" else "✔",
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        } else {
            val (badgeText, badgeColor) = when (task.status) {
                TaskStatus.MISSED -> "Missed" to FocusTheme.colors.danger
                TaskStatus.IN_PROGRESS -> "In Progress" to FocusTheme.colors.primaryAccent
                TaskStatus.SKIPPED -> "Skipped" to FocusTheme.colors.textSecondary
                else -> "Pending" to FocusTheme.colors.textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = if (FocusTheme.colors.isDark) 0.15f else 0.10f))
                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ChartLegend() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        LegendItem(color = FocusTheme.colors.success, label = "On Time")
        LegendItem(color = FocusTheme.colors.warning, label = "Late")
        LegendItem(color = FocusTheme.colors.danger, label = "Missed")
        LegendItem(color = FocusTheme.colors.primaryAccent.copy(alpha = 0.8f), label = "Pending")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = FocusTheme.colors.textSecondary,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun StatColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary, fontSize = 10.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 15.sp
            )
        )
    }
}
