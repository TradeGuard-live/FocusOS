package com.naman.focusos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun HabitGrid(
    habits: List<HabitEntity>,
    completions: List<HabitCompletionEntity>,
    yearMonth: YearMonth,
    onToggleDay: (habitId: Long, dateStr: String) -> Unit,
    isDayScheduled: (habit: HabitEntity, date: LocalDate) -> Boolean = { _, _ -> true },
    modifier: Modifier = Modifier
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()
    val isCurrentMonth = today.year == yearMonth.year && today.monthValue == yearMonth.monthValue
    val todayDay = today.dayOfMonth

    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Map: Pair(habitId, dayOfMonth) -> Boolean
    val completionSet = mutableSetOf<Pair<Long, Int>>()
    val completionsByHabit = completions.groupBy { it.habitId }

    for (c in completions) {
        if (c.isCompleted) {
            try {
                val date = LocalDate.parse(c.date, dateFormatter)
                if (date.year == yearMonth.year && date.monthValue == yearMonth.monthValue) {
                    completionSet.add(Pair(c.habitId, date.dayOfMonth))
                }
            } catch (e: Exception) {}
        }
    }

    val scrollState = rememberScrollState()

    FocusCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "HABIT MATRIX • ${yearMonth.month.name.take(3)} ${yearMonth.year}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${habits.size} Habits",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally Scrollable Grid
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    // Habit Names Column
                    Column {
                        // Header empty corner
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .width(130.dp)
                                .height(32.dp)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Habit / Day",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.textSecondary
                                )
                            )
                        }

                        // Habit Row Titles
                        habits.forEach { habit ->
                            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
                            val (currentStreak, _) = ProductivityIntelligenceEngine.calculateHabitStreak(habitCompletions, today)

                            val habitColor = try {
                                Color(android.graphics.Color.parseColor(habit.colorHex))
                            } catch (e: Exception) {
                                FocusTheme.colors.primaryAccent
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(38.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(habitColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FocusTheme.colors.textPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentStreak > 0) {
                                    Text(
                                        text = "🔥$currentStreak",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = FocusTheme.colors.warning
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Days 1..31 Columns
                    for (day in 1..daysInMonth) {
                        val isToday = isCurrentMonth && day == todayDay
                        val dateStr = yearMonth.atDay(day).format(dateFormatter)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Day Number Header Cell
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isToday) FocusTheme.colors.primaryAccent.copy(alpha = 0.2f) else Color.Transparent)
                            ) {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            // Habit Day Cells
                            habits.forEach { habit ->
                                val date = yearMonth.atDay(day)
                                val isScheduled = isDayScheduled(habit, date)
                                val isChecked = completionSet.contains(Pair(habit.id, day))
                                val habitColor = try {
                                    Color(android.graphics.Color.parseColor(habit.colorHex))
                                } catch (e: Exception) {
                                    FocusTheme.colors.success
                                }

                                if (!isScheduled) {
                                    // Disabled cell: routine is not scheduled on this day (e.g. Monday-only routine on other days)
                                    // Complete option is strictly disabled and styled as inactive
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FocusTheme.colors.surfaceVariant.copy(alpha = 0.25f))
                                            .border(
                                                width = 0.5.dp,
                                                color = FocusTheme.colors.border.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "—",
                                            color = FocusTheme.colors.textSecondary.copy(alpha = 0.35f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                } else {
                                    // Scheduled cell: Interactive, clickable, displays checkmark when done
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChecked) habitColor.copy(alpha = 0.85f) else FocusTheme.colors.surfaceVariant)
                                            .border(
                                                width = if (isToday) 1.dp else 0.5.dp,
                                                color = if (isToday) FocusTheme.colors.primaryAccent.copy(alpha = 0.6f) else FocusTheme.colors.border,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onToggleDay(habit.id, dateStr) }
                                    ) {
                                        if (isChecked) {
                                            Text(
                                                text = "✔",
                                                color = if (FocusTheme.colors.isDark) Color.Black else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
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
    }
}
