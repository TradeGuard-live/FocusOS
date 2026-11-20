package com.naman.focusos.ui.screens.routines

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.naman.focusos.data.model.HabitCompletionEntity
import com.naman.focusos.data.model.HabitEntity
import com.naman.focusos.data.model.HabitFrequency
import com.naman.focusos.domain.analytics.ProductivityIntelligenceEngine
import com.naman.focusos.domain.scheduler.HabitSyncEngine
import com.naman.focusos.ui.components.CreateHabitDialog
import com.naman.focusos.ui.components.FocusBunnyMascot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.HabitGrid
import com.naman.focusos.ui.components.MascotPose
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCreateHabitDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val todayStr = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // Group completions by habit ID for rapid lookup
    val completionsByHabit = remember(uiState.habitCompletions) {
        uiState.habitCompletions.groupBy { it.habitId }
    }

    // Excel File Picker via SAF
    val excelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                viewModel.importExcelHabits(stream)
            }
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateHabitDialog = true },
                containerColor = FocusTheme.colors.primaryAccent,
                contentColor = if (FocusTheme.colors.isDark) Color.Black else Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
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
                        text = "ROUTINES & ACTIVE HABITS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily Discipline & Consistency",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                }
            }

            // Import Message Toast Banner
            uiState.importMessage?.let { msg ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (FocusTheme.colors.isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = if (FocusTheme.colors.isDark) Color.White else Color(0xFF065F46),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Dismiss",
                                color = FocusTheme.colors.success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { viewModel.clearImportMessage() }
                            )
                        }
                    }
                }
            }

            // Month Navigation & Import Button
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.onPreviousMonth() }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = FocusTheme.colors.textSecondary)
                        }
                        Text(
                            text = "${uiState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.textPrimary)
                        )
                        IconButton(onClick = { viewModel.onNextMonth() }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = FocusTheme.colors.textSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            excelFilePicker.launch(arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "*/*"
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FocusTheme.colors.surfaceVariant,
                            contentColor = FocusTheme.colors.primaryAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Excel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Monthly Habit Grid Matrix (matching Habit_Tracker.xlsx)
            if (uiState.habits.isNotEmpty()) {
                item {
                    HabitGrid(
                        habits = uiState.habits,
                        completions = uiState.habitCompletions,
                        yearMonth = uiState.currentMonth,
                        onToggleDay = { habitId, dateStr ->
                            viewModel.toggleHabitDay(habitId, dateStr)
                        },
                        isDayScheduled = { habit, date ->
                            uiState.isHabitScheduledOnDate(habit, date)
                        }
                    )
                }
            }

            // Section Header: ACTIVE HABITS
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE HABITS (${uiState.habits.size})",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Auto-synced from tasks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (uiState.habits.isEmpty()) {
                item {
                    FocusCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) {
                            FocusBunnyMascot(
                                pose = MascotPose.THINKING,
                                size = 96.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No active habits or recurring routines yet.",
                                color = FocusTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tasks with Daily or Weekly recurrence automatically appear here. Single-day tasks are excluded.",
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showCreateHabitDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FocusTheme.colors.primaryAccent,
                                    contentColor = if (FocusTheme.colors.isDark) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Habit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.habits, key = { "habit_${it.id}" }) { habit ->
                    val habitComps = completionsByHabit[habit.id] ?: emptyList()
                    val (currentStreak, _) = ProductivityIntelligenceEngine.calculateHabitStreak(habitComps, today)
                    val isCompletedToday = habitComps.any { it.date == todayStr && it.isCompleted }
                    val isScheduledToday = uiState.isHabitScheduledOnDate(habit, today)

                    HabitCard(
                        habit = habit,
                        isCompletedToday = isCompletedToday,
                        isScheduledToday = isScheduledToday,
                        streak = currentStreak,
                        onToggleToday = { viewModel.toggleHabitDay(habit.id, todayStr) },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showCreateHabitDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateHabitDialog = false },
            onSave = {
                viewModel.createHabit(it)
                showCreateHabitDialog = false
            }
        )
    }
}

@Composable
fun HabitCard(
    habit: HabitEntity,
    isCompletedToday: Boolean = false,
    isScheduledToday: Boolean = true,
    streak: Int = 0,
    onToggleToday: () -> Unit = {},
    onDelete: () -> Unit
) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    } catch (e: Exception) {
        FocusTheme.colors.primaryAccent
    }

    val statusBg by animateColorAsState(
        targetValue = if (isCompletedToday) MintEmerald.copy(alpha = if (FocusTheme.colors.isDark) 0.20f else 0.12f)
        else FocusTheme.colors.surfaceVariant,
        animationSpec = tween(250),
        label = "habit_status_bg"
    )

    FocusCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Habit color indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(habitColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Habit Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = FocusTheme.colors.textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (streak > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔥$streak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.warning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                val frequencyLabel = if (habit.description.isNotBlank() && habit.description != "Auto-synced routine") {
                    habit.description
                } else {
                    when (habit.frequency) {
                        HabitFrequency.DAILY -> "Daily Routine"
                        HabitFrequency.SPECIFIC_DAYS -> HabitSyncEngine.formatWeeklySchedule(habit.daysOfWeekMask)
                        HabitFrequency.CUSTOM -> "Custom Routine"
                    }
                }
                Text(
                    text = frequencyLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Today's Status Pill (Interactive quick completion toggle if scheduled; disabled otherwise)
            if (!isScheduledToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FocusTheme.colors.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            1.dp,
                            FocusTheme.colors.border.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "—",
                            color = FocusTheme.colors.textSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Not Scheduled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textSecondary.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .border(
                            1.dp,
                            if (isCompletedToday) MintEmerald.copy(alpha = 0.5f) else FocusTheme.colors.border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleToday() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCompletedToday) "✔" else "⌛",
                            color = if (isCompletedToday) MintEmerald else FocusTheme.colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompletedToday) "Completed" else "Pending",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCompletedToday) MintEmerald else FocusTheme.colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = FocusTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
