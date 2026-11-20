package com.naman.focusos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.RecurrenceRuleEntity
import com.naman.focusos.data.model.RecurrenceType
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.scheduler.RecurrenceEngine
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import com.naman.focusos.ui.theme.WarmAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddTaskSheet(
    categories: List<CategoryEntity>,
    existingTasks: List<TaskEntity>,
    initialDate: LocalDate = LocalDate.now(),
    taskToEdit: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSaveTask: (task: TaskEntity, recurrenceRule: RecurrenceRuleEntity?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var scheduledDate by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.scheduledDate ?: initialDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var selectedCategoryId by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var selectedPriority by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.priority ?: Priority.MEDIUM)
    }

    // Schedule Time & Duration (separate from Deadline)
    var hasStartTime by remember(taskToEdit) {
        mutableStateOf(!taskToEdit?.startTime.isNullOrBlank())
    }
    var startTime by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.startTime ?: "")
    }
    var durationMinutes by remember(taskToEdit) {
        mutableIntStateOf(taskToEdit?.durationMinutes ?: 30)
    }

    // Hard Deadline (separate from Schedule Time)
    var hasDeadline by remember(taskToEdit) {
        mutableStateOf(!taskToEdit?.deadlineTime.isNullOrBlank())
    }
    var deadlineTime by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.deadlineTime ?: "")
    }

    // Task Notifications & Reminders (1st, 2nd, and 3rd notification if uncompleted)
    var hasNotifications by remember(taskToEdit) {
        mutableStateOf(!taskToEdit?.notificationTime1.isNullOrBlank())
    }
    var notificationTime1 by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.notificationTime1 ?: "")
    }
    var hasNotification2 by remember(taskToEdit) {
        mutableStateOf(!taskToEdit?.notificationTime2.isNullOrBlank())
    }
    var notificationTime2 by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.notificationTime2 ?: "")
    }
    var hasNotification3 by remember(taskToEdit) {
        mutableStateOf(!taskToEdit?.notificationTime3.isNullOrBlank())
    }
    var notificationTime3 by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.notificationTime3 ?: "")
    }

    var recurrenceType by remember(taskToEdit) { mutableStateOf(RecurrenceType.NONE) }
    var selectedWeekdays by remember(taskToEdit) { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // Mon-Fri default

    // If editing a task, automatically load its existing recurrence rule (e.g. Weekly weekdays)
    LaunchedEffect(taskToEdit) {
        val ruleId = taskToEdit?.recurrenceRuleId
        if (ruleId != null && ruleId > 0) {
            withContext(Dispatchers.IO) {
                val db = com.naman.focusos.data.db.FocusDatabase.getInstance(context)
                val existingRule = db.recurrenceRuleDao().getRuleById(ruleId)
                if (existingRule != null) {
                    withContext(Dispatchers.Main) {
                        recurrenceType = existingRule.type
                        selectedWeekdays = RecurrenceRuleEntity.getSelectedDays(existingRule.daysOfWeekMask).toSet()
                    }
                }
            }
        }
    }

    // Conflict detection
    var conflictingTasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }

    LaunchedEffect(title, scheduledDate, startTime, durationMinutes, hasStartTime) {
        if (hasStartTime && startTime.isNotBlank()) {
            val candidate = TaskEntity(
                id = taskToEdit?.id ?: 0L,
                title = title.ifBlank { "New Task" },
                scheduledDate = scheduledDate,
                startTime = startTime,
                durationMinutes = durationMinutes
            )
            conflictingTasks = RecurrenceEngine.detectConflicts(existingTasks, candidate)
        } else {
            conflictingTasks = emptyList()
        }
    }

    val isDark = FocusTheme.colors.isDark
    val dialogTheme = if (isDark) android.R.style.Theme_DeviceDefault_Dialog else android.R.style.Theme_DeviceDefault_Light_Dialog

    fun showNativeTimePicker(
        currentTime: String?,
        onTimePicked: (String) -> Unit
    ) {
        val initialHour: Int
        val initialMinute: Int
        if (!currentTime.isNullOrBlank()) {
            val parts = currentTime.split(":")
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 10
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            val now = LocalTime.now()
            initialHour = now.hour
            initialMinute = (now.minute / 5) * 5
        }

        val picker = android.app.TimePickerDialog(
            context,
            dialogTheme,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                onTimePicked(formatted)
            },
            initialHour,
            initialMinute,
            is24Hour
        )
        picker.show()
    }

    fun showNativeDatePicker(
        currentDateStr: String?,
        onDatePicked: (String) -> Unit
    ) {
        val currentLocalDate = try {
            if (!currentDateStr.isNullOrBlank()) LocalDate.parse(currentDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            else LocalDate.now()
        } catch (e: Exception) {
            LocalDate.now()
        }

        val picker = android.app.DatePickerDialog(
            context,
            dialogTheme,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                onDatePicked(picked.format(DateTimeFormatter.ISO_LOCAL_DATE))
            },
            currentLocalDate.year,
            currentLocalDate.monthValue - 1,
            currentLocalDate.dayOfMonth
        )
        picker.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (taskToEdit != null) "EDIT TASK" else "CREATE TASK",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    if (taskToEdit != null) {
                        Text(
                            text = "Task #${taskToEdit.id} • ${if (taskToEdit.status.isCompleted()) "Completed (Status Preserved)" else "Incomplete"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (taskToEdit.status.isCompleted()) FocusTheme.colors.success else FocusTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = FocusTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Complete DSA practice") },
                singleLine = true,
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                placeholder = { Text("Key focus points or requirements") },
                maxLines = 2,
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Quick Selectors
            Text(text = "Scheduled Date", style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                DateChip("Today", scheduledDate == today) { scheduledDate = today }
                DateChip("Tomorrow", scheduledDate == tomorrow) { scheduledDate = tomorrow }
                DateChip(
                    label = if (scheduledDate != today && scheduledDate != tomorrow) scheduledDate else "Other Date 📅",
                    isSelected = scheduledDate != today && scheduledDate != tomorrow
                ) {
                    showNativeDatePicker(scheduledDate) { picked ->
                        scheduledDate = picked
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(text = "Category", style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { FocusTheme.colors.primaryAccent }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                            .border(1.dp, if (isSelected) catColor else FocusTheme.colors.border, RoundedCornerShape(8.dp))
                            .clickable { selectedCategoryId = cat.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) catColor else FocusTheme.colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(text = "Priority", style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Priority.entries.forEach { p ->
                    val isSelected = p == selectedPriority
                    val (color, label) = when (p) {
                        Priority.CRITICAL -> Pair(FocusTheme.colors.danger, "Critical")
                        Priority.HIGH -> Pair(FocusTheme.colors.warning, "High")
                        Priority.MEDIUM -> Pair(FocusTheme.colors.primaryAccent, "Medium")
                        Priority.LOW -> Pair(FocusTheme.colors.textMuted, "Low")
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) color.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                            .border(1.dp, if (isSelected) color else FocusTheme.colors.border, RoundedCornerShape(8.dp))
                            .clickable { selectedPriority = p }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) color else FocusTheme.colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 1. SCHEDULE TIME & DURATION (Clock & Native TimePicker)
            // ----------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Schedule Time & Duration",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Planned performance time (sorts task in timeline)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = hasStartTime,
                    onCheckedChange = { checked ->
                        hasStartTime = checked
                        if (checked && startTime.isBlank()) {
                            startTime = "10:00"
                        }
                    },
                    colors = switchColors()
                )
            }

            if (hasStartTime) {
                Spacer(modifier = Modifier.height(8.dp))

                // Start Time Card / Input with Clock Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .border(1.dp, FocusTheme.colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            showNativeTimePicker(startTime) { picked ->
                                startTime = picked
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Clock",
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "START TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.primaryAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (startTime.isNotBlank()) {
                                val display = formatTimeDisplay(startTime, is24Hour)
                                if (is24Hour) display else "$display ($startTime)"
                            } else "Tap to choose time",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (startTime.isNotBlank()) FocusTheme.colors.textPrimary else FocusTheme.colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    if (startTime.isNotBlank()) {
                        IconButton(
                            onClick = {
                                showNativeTimePicker(startTime) { picked ->
                                    startTime = picked
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Time",
                                tint = FocusTheme.colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                startTime = ""
                                hasStartTime = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Time",
                                tint = FocusTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Planned Duration Configuration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (durationMinutes > 0) durationMinutes.toString() else "",
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            durationMinutes = digits.toIntOrNull()?.coerceIn(1, 1440) ?: 0
                        },
                        label = { Text("Duration (minutes)") },
                        placeholder = { Text("30") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    // Stepper buttons (-15 / +15)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusTheme.colors.surfaceVariant)
                                .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(8.dp))
                                .clickable { durationMinutes = (durationMinutes - 15).coerceAtLeast(5) }
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Text("-15m", color = FocusTheme.colors.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusTheme.colors.surfaceVariant)
                                .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(8.dp))
                                .clickable { durationMinutes = (durationMinutes + 15).coerceAtMost(1440) }
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Text("+15m", color = FocusTheme.colors.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duration Quick Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (durationMinutes == mins) FocusTheme.colors.primaryAccent.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                                .border(1.dp, if (durationMinutes == mins) FocusTheme.colors.primaryAccent else FocusTheme.colors.border, RoundedCornerShape(6.dp))
                            .clickable { durationMinutes = mins }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (durationMinutes == mins) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (durationMinutes == mins) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 2. HARD DEADLINE (Separate concept from Schedule Time)
            // ----------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Hard Deadline",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.danger,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Latest permitted completion time (triggers late/missed)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = hasDeadline,
                    onCheckedChange = { checked ->
                        hasDeadline = checked
                        if (checked && deadlineTime.isBlank()) {
                            deadlineTime = "12:00"
                        }
                    },
                    colors = switchColors()
                )
            }

            if (hasDeadline) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .border(1.dp, FocusTheme.colors.danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            showNativeTimePicker(deadlineTime) { picked ->
                                deadlineTime = picked
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Deadline Clock",
                        tint = FocusTheme.colors.danger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEADLINE TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (deadlineTime.isNotBlank()) {
                                val display = formatTimeDisplay(deadlineTime, is24Hour)
                                if (is24Hour) display else "$display ($deadlineTime)"
                            } else "Tap to choose deadline",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (deadlineTime.isNotBlank()) FocusTheme.colors.textPrimary else FocusTheme.colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    if (deadlineTime.isNotBlank()) {
                        IconButton(
                            onClick = {
                                showNativeTimePicker(deadlineTime) { picked ->
                                    deadlineTime = picked
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Deadline",
                                tint = FocusTheme.colors.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                deadlineTime = ""
                                hasDeadline = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Deadline",
                                tint = FocusTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 3. TASK NOTIFICATIONS & REMINDERS (1st, 2nd & 3rd if uncompleted)
            // ----------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task Notifications & Reminders",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Custom notification time with 2nd and 3rd alerts if task is not completed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = hasNotifications,
                    onCheckedChange = { checked ->
                        hasNotifications = checked
                        if (checked && notificationTime1.isBlank()) {
                            notificationTime1 = if (hasStartTime && startTime.isNotBlank()) startTime else "10:00"
                        }
                    },
                    colors = switchColors()
                )
            }

            if (hasNotifications) {
                Spacer(modifier = Modifier.height(8.dp))

                // --- 1ST NOTIFICATION ---
                NotificationTimeRow(
                    label = "1ST NOTIFICATION",
                    sublabel = "Primary alert time",
                    time = notificationTime1,
                    accentColor = FocusTheme.colors.primaryAccent,
                    is24Hour = is24Hour,
                    onPickTime = {
                        showNativeTimePicker(notificationTime1.ifBlank { startTime.ifBlank { "10:00" } }) { picked ->
                            notificationTime1 = picked
                        }
                    },
                    onClear = {
                        notificationTime1 = ""
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // --- 2ND NOTIFICATION (IF MISSED / UNCOMPLETED) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "2nd Notification (If Missed)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.warning,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Switch(
                        checked = hasNotification2,
                        onCheckedChange = { checked ->
                            hasNotification2 = checked
                            if (checked && notificationTime2.isBlank()) {
                                notificationTime2 = addMinutesToTime(notificationTime1.ifBlank { startTime }, 30)
                            }
                        },
                        colors = switchColors()
                    )
                }

                if (hasNotification2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NotificationTimeRow(
                        label = "2ND NOTIFICATION",
                        sublabel = "Fires only if task is not completed yet",
                        time = notificationTime2,
                        accentColor = FocusTheme.colors.warning,
                        is24Hour = is24Hour,
                        onPickTime = {
                            showNativeTimePicker(notificationTime2.ifBlank { addMinutesToTime(notificationTime1, 30) }) { picked ->
                                notificationTime2 = picked
                            }
                        },
                        onClear = {
                            notificationTime2 = ""
                            hasNotification2 = false
                        }
                    )

                    // Quick offset suggestions (+15m, +30m, +1h from notification 1)
                    if (notificationTime1.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15L, 30L, 60L).forEach { mins ->
                                val offsetTime = addMinutesToTime(notificationTime1, mins)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (notificationTime2 == offsetTime) FocusTheme.colors.warning.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                                        .border(1.dp, if (notificationTime2 == offsetTime) FocusTheme.colors.warning else FocusTheme.colors.border, RoundedCornerShape(6.dp))
                                        .clickable { notificationTime2 = offsetTime }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+${mins}m ($offsetTime)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (notificationTime2 == offsetTime) FocusTheme.colors.warning else FocusTheme.colors.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- 3RD NOTIFICATION (FINAL ALERT IF MISSED) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "3rd Notification (Final If Missed)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.danger,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Switch(
                        checked = hasNotification3,
                        onCheckedChange = { checked ->
                            hasNotification3 = checked
                            if (checked && notificationTime3.isBlank()) {
                                notificationTime3 = addMinutesToTime(
                                    if (hasNotification2 && notificationTime2.isNotBlank()) notificationTime2 else notificationTime1.ifBlank { startTime },
                                    30
                                )
                            }
                        },
                        colors = switchColors()
                    )
                }

                if (hasNotification3) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NotificationTimeRow(
                        label = "3RD NOTIFICATION",
                        sublabel = "Final alert if task remains incomplete",
                        time = notificationTime3,
                        accentColor = FocusTheme.colors.danger,
                        is24Hour = is24Hour,
                        onPickTime = {
                            showNativeTimePicker(notificationTime3.ifBlank { addMinutesToTime(notificationTime2.ifBlank { notificationTime1 }, 30) }) { picked ->
                                notificationTime3 = picked
                            }
                        },
                        onClear = {
                            notificationTime3 = ""
                            hasNotification3 = false
                        }
                    )

                    // Quick offset suggestions (+30m, +1h, +2h)
                    val baseTime = if (hasNotification2 && notificationTime2.isNotBlank()) notificationTime2 else notificationTime1
                    if (baseTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30L, 60L, 120L).forEach { mins ->
                                val offsetTime = addMinutesToTime(baseTime, mins)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (notificationTime3 == offsetTime) FocusTheme.colors.danger.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                                        .border(1.dp, if (notificationTime3 == offsetTime) FocusTheme.colors.danger else FocusTheme.colors.border, RoundedCornerShape(6.dp))
                                        .clickable { notificationTime3 = offsetTime }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+${mins}m ($offsetTime)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (notificationTime3 == offsetTime) FocusTheme.colors.danger else FocusTheme.colors.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recurrence Section (Fully accessible & editable for both new and existing tasks)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (taskToEdit != null) "Repeat / Recurrence (Updates All Occurrences)" else "Repeat / Recurrence",
                style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.primaryAccent)
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.entries.forEach { r ->
                    val isSelected = r == recurrenceType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) FocusTheme.colors.primaryAccent.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
                            .border(1.dp, if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border, RoundedCornerShape(8.dp))
                            .clickable { recurrenceType = r }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = r.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // If Weekly recurrence selected, show weekday toggles
            if (recurrenceType == RecurrenceType.WEEKLY) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                    days.forEach { (label, dayNum) ->
                        val isDaySelected = selectedWeekdays.contains(dayNum)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDaySelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.surfaceVariant)
                                .border(1.dp, if (isDaySelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedWeekdays = if (isDaySelected) selectedWeekdays - dayNum else selectedWeekdays + dayNum
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) (if (FocusTheme.colors.isDark) Color.Black else Color.White) else FocusTheme.colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Conflict Warning Banner
            AnimatedVisibility(visible = conflictingTasks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (FocusTheme.colors.isDark) Color(0xFF451A03) else Color(0xFFFEF3C7))
                        .border(1.dp, FocusTheme.colors.warning, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = FocusTheme.colors.warning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conflict: Overlaps with '${conflictingTasks.first().title}' (${conflictingTasks.first().startTime}). You can proceed or adjust time.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (FocusTheme.colors.isDark) FocusTheme.colors.warning else Color(0xFF92400E),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save / Update Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalTask = if (taskToEdit != null) {
                            taskToEdit.copy(
                                title = title.trim(),
                                description = description.trim(),
                                categoryId = selectedCategoryId,
                                priority = selectedPriority,
                                scheduledDate = scheduledDate,
                                startTime = if (hasStartTime && startTime.isNotBlank()) startTime.trim() else null,
                                durationMinutes = durationMinutes.coerceAtLeast(1),
                                deadlineTime = if (hasDeadline && deadlineTime.isNotBlank()) deadlineTime.trim() else null,
                                notificationTime1 = if (hasNotifications && notificationTime1.isNotBlank()) notificationTime1.trim() else null,
                                notificationTime2 = if (hasNotifications && hasNotification2 && notificationTime2.isNotBlank()) notificationTime2.trim() else null,
                                notificationTime3 = if (hasNotifications && hasNotification3 && notificationTime3.isNotBlank()) notificationTime3.trim() else null,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            TaskEntity(
                                title = title.trim(),
                                description = description.trim(),
                                categoryId = selectedCategoryId,
                                priority = selectedPriority,
                                scheduledDate = scheduledDate,
                                startTime = if (hasStartTime && startTime.isNotBlank()) startTime.trim() else null,
                                durationMinutes = durationMinutes.coerceAtLeast(1),
                                deadlineTime = if (hasDeadline && deadlineTime.isNotBlank()) deadlineTime.trim() else null,
                                notificationTime1 = if (hasNotifications && notificationTime1.isNotBlank()) notificationTime1.trim() else null,
                                notificationTime2 = if (hasNotifications && hasNotification2 && notificationTime2.isNotBlank()) notificationTime2.trim() else null,
                                notificationTime3 = if (hasNotifications && hasNotification3 && notificationTime3.isNotBlank()) notificationTime3.trim() else null
                            )
                        }

                        val rule = if (recurrenceType != RecurrenceType.NONE) {
                            RecurrenceRuleEntity(
                                id = taskToEdit?.recurrenceRuleId ?: 0L,
                                type = recurrenceType,
                                daysOfWeekMask = RecurrenceRuleEntity.createWeeklyMask(selectedWeekdays.toList()),
                                startDate = scheduledDate
                            )
                        } else null

                        onSaveTask(finalTask, rule)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primaryAccent,
                    contentColor = if (FocusTheme.colors.isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (taskToEdit != null) "Save Changes"
                           else if (conflictingTasks.isNotEmpty()) "Save Task (Override Conflict)"
                           else "Create Focus Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

fun formatTimeDisplay(timeStr: String?, is24H: Boolean): String {
    if (timeStr.isNullOrBlank()) return ""
    return try {
        val time = LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        if (is24H) {
            time.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            time.format(DateTimeFormatter.ofPattern("hh:mm a"))
        }
    } catch (e: Exception) {
        timeStr
    }
}

@Composable
fun DateChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) FocusTheme.colors.primaryAccent.copy(alpha = 0.25f) else FocusTheme.colors.surfaceVariant)
            .border(1.dp, if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

fun addMinutesToTime(timeStr: String?, minutesToAdd: Long): String {
    if (timeStr.isNullOrBlank()) return "10:00"
    return try {
        val time = LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        time.plusMinutes(minutesToAdd).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        timeStr
    }
}

@Composable
fun NotificationTimeRow(
    label: String,
    sublabel: String,
    time: String,
    accentColor: Color,
    is24Hour: Boolean,
    onPickTime: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FocusTheme.colors.surfaceVariant)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onPickTime() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notification Bell",
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = if (time.isNotBlank()) {
                    val display = formatTimeDisplay(time, is24Hour)
                    if (is24Hour) display else "$display ($time)"
                } else "Tap to choose notification time",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (time.isNotBlank()) FocusTheme.colors.textPrimary else FocusTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontSize = 10.sp
                )
            )
        }

        if (time.isNotBlank()) {
            IconButton(onClick = onPickTime, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Time",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Time",
                    tint = FocusTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
