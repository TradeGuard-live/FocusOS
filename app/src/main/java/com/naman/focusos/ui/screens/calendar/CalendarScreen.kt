// Cleaned CalendarScreen.kt after fixing syntax errors
package com.naman.focusos.ui.screens.calendar

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.ui.components.FocusBunnyMascot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.MascotPose
import com.naman.focusos.ui.components.QuickAddTaskSheet
import com.naman.focusos.ui.components.TaskCard
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var showRecurringChoiceDialog by remember { mutableStateOf(false) }
    var showPermanentConfirmDialog by remember { mutableStateOf(false) }

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
            // View Mode Selector Tabs
            item {
                CalendarViewSelector(
                    currentMode = uiState.viewMode,
                    onModeSelected = { viewModel.setViewMode(it) }
                )
            }

            // Month Navigation Header
            item {
                CalendarMonthHeader(
                    currentMonth = uiState.currentMonth,
                    onPrev = { viewModel.onPreviousMonth() },
                    onNext = { viewModel.onNextMonth() }
                )
            }

            // Interactive Calendar Grid
            item {
                FocusCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        WeekdayHeader()
                        Spacer(modifier = Modifier.height(8.dp))
                        CalendarMonthGrid(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            tasksByDate = uiState.tasksByDate,
                            onDateClick = { viewModel.onDateSelected(it) }
                        )
                    }
                }
            }

            // Header for selected date
            item {
                val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = uiState.selectedDate.format(formatter),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                    Text(
                        text = "${uiState.selectedDateTasks.size} Tasks",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent)
                    )
                }
            }

            if (uiState.selectedDateTasks.isEmpty()) {
                item {
                    FocusCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) {
                            FocusBunnyMascot(
                                pose = MascotPose.SLEEPING,
                                size = 88.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No tasks scheduled on this day.",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusTheme.colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Take a break, or tap + to schedule a focus task or routine.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            } else {
                items(uiState.selectedDateTasks, key = { "cal_task_${it.id}" }) { task ->
                    val cat = categoryMap[task.categoryId]
                    TaskCard(
                        task = task,
                        categoryName = cat?.name ?: "Focus",
                        categoryColorHex = cat?.colorHex ?: "#38BDF8",
                        onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                        onEdit = { taskToEdit = task },
                        onDelete = {
                            taskToDelete = task
                            val isRecurring = task.recurrenceRuleId != null && task.recurrenceRuleId > 0
                            if (isRecurring) {
                                showRecurringChoiceDialog = true
                            } else {
                                showPermanentConfirmDialog = true
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add/Edit sheet
    if (showAddTaskSheet || taskToEdit != null) {
        QuickAddTaskSheet(
            categories = uiState.categories,
            existingTasks = uiState.selectedDateTasks,
            initialDate = uiState.selectedDate,
            taskToEdit = taskToEdit,
            onDismiss = {
                showAddTaskSheet = false
                taskToEdit = null
            },
            onSaveTask = { task, rule ->
                if (taskToEdit != null) {
                    viewModel.updateTask(task, rule)
                } else {
                    viewModel.createTask(task, rule)
                }
                showAddTaskSheet = false
                taskToEdit = null
            }
        )
    }

    // 1. Recurring Choice Dialog
    if (showRecurringChoiceDialog && taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = {
                showRecurringChoiceDialog = false
                taskToDelete = null
            },
            title = {
                Text(
                    text = "Delete Recurring Task",
                    fontWeight = FontWeight.Bold,
                    color = FocusTheme.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This task belongs to a recurring routine with multiple scheduled occurrences. Choose an option:",
                        color = FocusTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            viewModel.deleteTask(task)
                            showRecurringChoiceDialog = false
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FocusTheme.colors.surfaceVariant,
                            contentColor = FocusTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete this occurrence only")
                    }
                    Button(
                        onClick = {
                            showRecurringChoiceDialog = false
                            showPermanentConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonCoral,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete entire task permanently", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRecurringChoiceDialog = false
                        taskToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = FocusTheme.colors.textSecondary)
                }
            },
            containerColor = FocusTheme.colors.surface
        )
    }

    // 2. Permanent Deletion Confirmation Dialog
    if (showPermanentConfirmDialog && taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = {
                showPermanentConfirmDialog = false
                taskToDelete = null
            },
            title = {
                Text(
                    text = "Delete this task permanently?",
                    fontWeight = FontWeight.Bold,
                    color = FocusTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This task will be removed from all calendar dates and task lists. This action cannot be undone.",
                    color = FocusTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (task.recurrenceRuleId != null && task.recurrenceRuleId > 0) {
                            viewModel.deleteRecurringTaskPermanently(task)
                        } else {
                            viewModel.deleteTask(task)
                        }
                        showPermanentConfirmDialog = false
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonCoral,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPermanentConfirmDialog = false
                        taskToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = FocusTheme.colors.textSecondary)
                }
            },
            containerColor = FocusTheme.colors.surface
        )
    }
}

@Composable
fun CalendarViewSelector(
    currentMode: CalendarViewMode,
    onModeSelected: (CalendarViewMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FocusTheme.colors.surfaceVariant)
            .padding(4.dp)
    ) {
        CalendarViewMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) FocusTheme.colors.primaryAccent else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) (if (FocusTheme.colors.isDark) Color.Black else Color.White) else FocusTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun CalendarMonthHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = FocusTheme.colors.textPrimary
                )
            )
        }

        Row {
            IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = FocusTheme.colors.textSecondary)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = FocusTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
fun WeekdayHeader() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CalendarMonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<String, List<TaskEntity>>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0 for Mon .. 6 for Sun
    val totalDaysInMonth = currentMonth.lengthOfMonth()

    val totalCells = ((dayOfWeekOffset + totalDaysInMonth + 6) / 7) * 7
    val rows = totalCells / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - dayOfWeekOffset + 1

                    if (dayNum in 1..totalDaysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val tasks = tasksByDate[dateStr] ?: emptyList()
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        CalendarCell(
                            dayNumber = dayNum,
                            tasksCount = tasks.size,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty spacer cell
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    dayNumber: Int,
    tasksCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = FocusTheme.colors.isDark
    val todayBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) FocusTheme.colors.primaryAccent else if (isToday) todayBg else Color.Transparent)
            .then(
                if (isToday && !isSelected) Modifier.border(1.dp, FocusTheme.colors.primaryAccent, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) (if (isDark) Color.Black else Color.White) else FocusTheme.colors.textPrimary,
                    fontSize = 13.sp
                )
            )

            if (tasksCount > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) (if (isDark) Color.Black else Color.White) else FocusTheme.colors.success)
                )
            }
        }
    }
}
