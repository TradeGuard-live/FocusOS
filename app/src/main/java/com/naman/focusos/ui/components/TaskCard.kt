package com.naman.focusos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.Priority
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.data.model.TaskStatus
import com.naman.focusos.ui.theme.CrimsonCoral
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald
import com.naman.focusos.ui.theme.WarmAmber
import java.time.LocalDateTime

@Composable
fun TaskCard(
    task: TaskEntity,
    categoryName: String = "Focus",
    categoryColorHex: String = "#38BDF8",
    onToggleComplete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val effectiveStatus = task.evaluateEffectiveStatus(LocalDateTime.now())
    val isCompleted = effectiveStatus.isCompleted()

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(categoryColorHex))
    } catch (e: Exception) {
        FocusTheme.colors.primaryAccent
    }

    val checkboxBg by animateColorAsState(
        targetValue = if (isCompleted) FocusTheme.colors.success else Color.Transparent,
        animationSpec = tween(300),
        label = "task_checkbox_bg"
    )

    val checkboxBorder by animateColorAsState(
        targetValue = if (isCompleted) FocusTheme.colors.success else FocusTheme.colors.border,
        animationSpec = tween(300),
        label = "task_checkbox_border"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.03f else 1.0f,
        animationSpec = tween(200),
        label = "task_scale"
    )

    val cardBg = if (effectiveStatus == TaskStatus.IN_PROGRESS) {
        if (FocusTheme.colors.isDark) Color(0xFF162544) else Color(0xFFE0F2FE)
    } else {
        FocusTheme.colors.surface
    }

    val cardBorder = if (effectiveStatus == TaskStatus.IN_PROGRESS) {
        FocusTheme.colors.primaryAccent.copy(alpha = 0.5f)
    } else {
        FocusTheme.colors.border
    }

    FocusCard(
        modifier = modifier.scale(scale),
        backgroundColor = cardBg,
        borderColor = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Checkbox
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(checkboxBg)
                        .clickable { onToggleComplete() }
                        .padding(2.dp)
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(FocusTheme.colors.surfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Timing
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) FocusTheme.colors.textMuted else FocusTheme.colors.textPrimary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge or Menu
                StatusPill(effectiveStatus)

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = FocusTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isCompleted) "Mark Incomplete" else "Mark Complete") },
                            onClick = {
                                menuExpanded = false
                                onToggleComplete()
                            }
                        )
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                        }
                        if (onSkip != null && !isCompleted) {
                            DropdownMenuItem(
                                text = { Text("Skip Task") },
                                onClick = {
                                    menuExpanded = false
                                    onSkip()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = CrimsonCoral) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Badges: Category, Timing, Deadline, Priority
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = if (FocusTheme.colors.isDark) 0.15f else 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Time / Duration Tag
                if (!task.startTime.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FocusTheme.colors.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = FocusTheme.colors.primaryAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.startTime} (${task.durationMinutes}m)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FocusTheme.colors.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = FocusTheme.colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Deadline Tag
                if (task.deadlineTime != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (FocusTheme.colors.isDark) Color(0xFF3B1D28) else Color(0xFFFEE2E2))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Due ${task.deadlineTime}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CrimsonCoral,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Notification Reminder Tag
                if (!task.notificationTime1.isNullOrBlank()) {
                    val notifCount = listOfNotNull(task.notificationTime1, task.notificationTime2, task.notificationTime3)
                        .filter { it.isNotBlank() }
                        .size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FocusTheme.colors.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = FocusTheme.colors.primaryAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (notifCount > 1) "${task.notificationTime1} ($notifCount)" else task.notificationTime1,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusTheme.colors.textPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Priority Badge
                PriorityBadge(task.priority)
            }
        }
    }
}

@Composable
fun StatusPill(status: TaskStatus) {
    val (bgColor, textColor, label) = when (status) {
        TaskStatus.COMPLETED_ON_TIME -> Triple(FocusTheme.colors.successContainer, FocusTheme.colors.success, "On Time")
        TaskStatus.COMPLETED_LATE -> Triple(FocusTheme.colors.warningContainer, FocusTheme.colors.warning, "Late")
        TaskStatus.MISSED -> Triple(FocusTheme.colors.errorContainer, FocusTheme.colors.error, "Missed")
        TaskStatus.IN_PROGRESS -> Triple(FocusTheme.colors.primaryAccentContainer, FocusTheme.colors.primaryAccent, "Active")
        TaskStatus.UPCOMING -> Triple(FocusTheme.colors.surfaceSecondary, FocusTheme.colors.textSecondary, "Upcoming")
        TaskStatus.SKIPPED -> Triple(FocusTheme.colors.surfaceSecondary, FocusTheme.colors.textTertiary, "Skipped")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    val (color, text) = when (priority) {
        Priority.CRITICAL -> Pair(CrimsonCoral, "CRITICAL")
        Priority.HIGH -> Pair(WarmAmber, "HIGH")
        Priority.MEDIUM -> Pair(FocusTheme.colors.primaryAccent, "MED")
        Priority.LOW -> Pair(FocusTheme.colors.textMuted, "LOW")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = if (FocusTheme.colors.isDark) 0.15f else 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        )
    }
}
