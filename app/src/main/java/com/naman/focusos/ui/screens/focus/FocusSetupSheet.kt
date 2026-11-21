package com.naman.focusos.ui.screens.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.CategoryEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.focus.AndroidAppUsageProvider
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.outlinedColors
import com.naman.focusos.ui.components.switchColors
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun FocusSetupView(
    availableTasks: List<TaskEntity>,
    availableCategories: List<CategoryEntity>,
    hasUsagePermission: Boolean,
    defaultDurationMinutes: Int,
    onStartSession: (title: String, description: String?, linkedTaskId: Long?, category: String?, durationMinutes: Int, monitoringEnabled: Boolean) -> Unit,
    onRequestUsagePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var targetMinutes by remember { mutableIntStateOf(defaultDurationMinutes) }
    var monitoringEnabled by remember { mutableStateOf(true) }
    var isCustomDuration by remember { mutableStateOf(false) }
    var customDurationText by remember { mutableStateOf("45") }

    var taskDropdownExpanded by remember { mutableStateOf(false) }

    val durationPresets = listOf(15, 25, 30, 45, 60, 90, 0) // 0 = open-ended

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Session Goal & Title
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "START FOCUS SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you focusing on?") },
                    placeholder = { Text("e.g. Deep Code Refactoring, Reading...") },
                    singleLine = true,
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Goal (Optional)") },
                    placeholder = { Text("e.g. Finish Phase 1 & review tests") },
                    maxLines = 2,
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section: Target Duration Presets
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )

                    Text(
                        text = if (targetMinutes == 0) "Open-Ended" else "$targetMinutes mins",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(durationPresets) { preset ->
                        val isSelected = (!isCustomDuration && targetMinutes == preset)
                        val label = if (preset == 0) "Open-ended" else "${preset}m"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) FocusTheme.colors.primaryAccent
                                    else FocusTheme.colors.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isCustomDuration = false
                                    targetMinutes = preset
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) FocusTheme.colors.onPrimaryAccent else FocusTheme.colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCustomDuration) FocusTheme.colors.primaryAccent
                                    else FocusTheme.colors.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCustomDuration) FocusTheme.colors.primaryAccent else FocusTheme.colors.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isCustomDuration = true
                                    targetMinutes = customDurationText.toIntOrNull() ?: 45
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isCustomDuration) FocusTheme.colors.onPrimaryAccent else FocusTheme.colors.textPrimary,
                                    fontWeight = if (isCustomDuration) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isCustomDuration) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customDurationText,
                            onValueChange = {
                                customDurationText = it
                                targetMinutes = it.toIntOrNull()?.coerceIn(1, 480) ?: 45
                            },
                            label = { Text("Custom duration (minutes)") },
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Section: Link Existing Task
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LINK TO AN EXISTING TASK (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                val selectedTask = availableTasks.find { it.id == selectedTaskId }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(12.dp))
                        .clickable { taskDropdownExpanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTask?.title ?: "Select a pending task...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (selectedTask != null) FocusTheme.colors.textPrimary else FocusTheme.colors.textSecondary
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = FocusTheme.colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = taskDropdownExpanded,
                        onDismissRequest = { taskDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Free focus session)") },
                            onClick = {
                                selectedTaskId = null
                                taskDropdownExpanded = false
                            }
                        )
                        availableTasks.forEach { task ->
                            DropdownMenuItem(
                                text = { Text(task.title) },
                                onClick = {
                                    selectedTaskId = task.id
                                    if (title.isBlank()) {
                                        title = task.title
                                    }
                                    taskDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section: Distraction Monitoring & Usage Access
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distraction Monitoring",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FocusTheme.colors.textPrimary
                            )
                        )
                        Text(
                            text = "Track app switches and identify distractions automatically",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocusTheme.colors.textSecondary
                            )
                        )
                    }

                    Switch(
                        checked = monitoringEnabled,
                        onCheckedChange = { monitoringEnabled = it },
                        colors = switchColors()
                    )
                }

                if (monitoringEnabled && !hasUsagePermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusTheme.colors.warningContainer)
                            .border(1.dp, FocusTheme.colors.warning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FocusTheme.colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usage Access Required",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FocusTheme.colors.textPrimary
                                    )
                                )
                                Text(
                                    text = "Allow Focus OS in Settings to detect foreground applications during your session.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = FocusTheme.colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FocusTheme.colors.warning)
                                    .clickable { onRequestUsagePermission() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Grant",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FocusTheme.colors.onWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Start Button
        Button(
            onClick = {
                val effectiveTitle = if (title.isNotBlank()) title else {
                    selectedTaskId?.let { id -> availableTasks.find { it.id == id }?.title } ?: "Focus Session"
                }
                onStartSession(
                    effectiveTitle,
                    description.ifBlank { null },
                    selectedTaskId,
                    selectedCategory,
                    targetMinutes,
                    monitoringEnabled
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = FocusTheme.colors.primaryAccent,
                contentColor = FocusTheme.colors.onPrimaryAccent
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START FOCUS SESSION",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
