package com.naman.focusos.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TimetableEntryEntity
import com.naman.focusos.ui.components.FocusBunnyMascot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.MascotPose
import com.naman.focusos.ui.components.ProgressRing
import com.naman.focusos.ui.components.QuickAddTaskSheet
import com.naman.focusos.ui.components.TaskCard
import com.naman.focusos.ui.components.outlinedColors
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }

    val categoryMap = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = FocusTheme.colors.primaryAccent,
                contentColor = if (FocusTheme.colors.isDark) Color.Black else Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
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
            // 1. Futuristic Header & Time-based Greeting
            item {
                TodayHeaderSection(uiState.selectedDate)
            }

            // 2. Daily Productivity Ring & Summary Banner
            item {
                DailyProgressSection(
                    stats = uiState.dayStats,
                    completedCount = uiState.completedTasks.size,
                    totalCount = uiState.tasks.size
                )
            }

            // 3. Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search today's tasks...", color = FocusTheme.colors.textMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = FocusTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Daily Timetable Horizontal Strip
            if (uiState.timetableEntries.isNotEmpty()) {
                item {
                    TimetableStrip(uiState.timetableEntries, categoryMap)
                }
            }

            // 5. Active Task (If in progress right now)
            uiState.activeTask?.let { active ->
                item {
                    SectionTitle(title = "ACTIVE NOW", color = FocusTheme.colors.primaryAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    val cat = categoryMap[active.categoryId]
                    TaskCard(
                        task = active,
                        categoryName = cat?.name ?: "Focus",
                        categoryColorHex = cat?.colorHex ?: "#38BDF8",
                        onToggleComplete = { viewModel.toggleTaskComplete(active.id) },
                        onEdit = null,
                        onDelete = null,
                        onSkip = { viewModel.skipTask(active.id) }
                    )
                }
            }

            // 6. Overdue Tasks Section
            if (uiState.overdueTasks.isNotEmpty()) {
                item {
                    SectionTitle(title = "OVERDUE TASKS (${uiState.overdueTasks.size})", color = CrimsonCoral)
                }
                items(uiState.overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    val cat = categoryMap[task.categoryId]
                    TaskCard(
                        task = task,
                        categoryName = cat?.name ?: "Focus",
                        categoryColorHex = cat?.colorHex ?: "#EF4444",
                        onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                        onEdit = null,
                        onDelete = null,
                        onSkip = { viewModel.skipTask(task.id) }
                    )
                }
            }

            // 7. Upcoming Tasks Section
            item {
                SectionTitle(
                    title = "TODAY'S TASKS (${uiState.upcomingTasks.size})",
                    color = FocusTheme.colors.textPrimary
                )
            }

            if (uiState.upcomingTasks.isEmpty() && uiState.overdueTasks.isEmpty()) {
                item {
                    EmptyDayTasksCard { showAddTaskSheet = true }
                }
            } else {
                items(uiState.upcomingTasks, key = { "upcoming_${it.id}" }) { task ->
                    val cat = categoryMap[task.categoryId]
                    TaskCard(
                        task = task,
                        categoryName = cat?.name ?: "Focus",
                        categoryColorHex = cat?.colorHex ?: "#38BDF8",
                        onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                        onEdit = null,
                        onDelete = null,
                        onSkip = { viewModel.skipTask(task.id) }
                    )
                }
            }

            // 8. Completed Tasks Section (Collapsible)
            if (uiState.completedTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCompletedCollapsed() }
                            .padding(vertical = 4.dp)
                    ) {
                        SectionTitle(
                            title = "COMPLETED (${uiState.completedTasks.size})",
                            color = FocusTheme.colors.success
                        )
                        Icon(
                            imageVector = if (uiState.isCompletedCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle",
                            tint = FocusTheme.colors.textSecondary
                        )
                    }
                }

                if (!uiState.isCompletedCollapsed) {
                    items(uiState.completedTasks, key = { "completed_${it.id}" }) { task ->
                        val cat = categoryMap[task.categoryId]
                        TaskCard(
                            task = task,
                            categoryName = cat?.name ?: "Focus",
                            categoryColorHex = cat?.colorHex ?: "#10B981",
                            onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                            onEdit = null,
                            onDelete = null
                        )
                    }
                }
            }

            // Extra space at bottom for floating action button & bottom bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddTaskSheet) {
        QuickAddTaskSheet(
            categories = uiState.categories,
            existingTasks = uiState.tasks,
            initialDate = uiState.selectedDate,
            taskToEdit = null,
            onDismiss = {
                showAddTaskSheet = false
            },
            onSaveTask = { task, rule ->
                viewModel.createTask(task, rule)
                showAddTaskSheet = false
            }
        )
    }
}

@Composable
fun TodayHeaderSection(selectedDate: LocalDate) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    val dayName = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthName = selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dateText = "$dayName, $monthName ${selectedDate.dayOfMonth}"

    Column {
        Text(
            text = "FOCUS OS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = FocusTheme.colors.primaryAccent,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = FocusTheme.colors.textPrimary
            )
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = FocusTheme.colors.textSecondary
            )
        )
    }
}

@Composable
fun DailyProgressSection(
    stats: com.naman.focusos.domain.analytics.DayStats?,
    completedCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    FocusCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ProgressRing(
                progress = progress,
                completedCount = completedCount,
                totalCount = totalCount
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Productivity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                val remaining = (totalCount - completedCount).coerceAtLeast(0)
                Text(
                    text = if (totalCount == 0) "No tasks scheduled for today."
                    else if (remaining == 0) "All tasks completed! Peak focus achieved ✨"
                    else "$remaining tasks remaining today.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats?.let { s ->
                        Text(text = "✔ ${s.completedOnTime} on-time", color = FocusTheme.colors.success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (s.completedLate > 0) {
                            Text(text = "⏰ ${s.completedLate} late", color = FocusTheme.colors.warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (s.missed > 0) {
                            Text(text = "✖ ${s.missed} missed", color = FocusTheme.colors.danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableStrip(
    entries: List<TimetableEntryEntity>,
    categoryMap: Map<Long, CategoryEntity>
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = FocusTheme.colors.primaryAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            SectionTitle(title = "DAILY TIMETABLE", color = FocusTheme.colors.primaryAccent)
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            entries.forEach { entry ->
                val cat = categoryMap[entry.categoryId]
                val catColor = try { Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#38BDF8")) } catch (e: Exception) { FocusTheme.colors.primaryAccent }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "${entry.startTime} – ${entry.getEndTime()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = catColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDayTasksCard(onAddTask: () -> Unit) {
    FocusCard(
        onClick = onAddTask
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            FocusBunnyMascot(
                pose = MascotPose.HAPPY,
                size = 100.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "All clear for today!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No tasks for today. Tap below to schedule a focus task, routine or deadline.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocusTheme.colors.textSecondary, fontSize = 12.sp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
}
