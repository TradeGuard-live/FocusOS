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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.theme.FocusTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FocusHistoryView(
    sessions: List<FocusSessionEntity>,
    activeFilter: HistoryFilter,
    inspectedSession: FocusSessionEntity?,
    inspectedPauses: List<FocusPauseEntity>,
    inspectedUsages: List<FocusAppUsageEntity>,
    inspectedEvents: List<FocusSessionEventEntity>,
    onFilterSelected: (HistoryFilter) -> Unit,
    onInspectSession: (FocusSessionEntity?) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sessionToDelete by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HistoryFilter.entries) { filter ->
                val isSelected = filter == activeFilter
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
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) FocusTheme.colors.onPrimaryAccent else FocusTheme.colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = FocusTheme.colors.textSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No focus sessions found",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = FocusTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isExpanded = inspectedSession?.id == session.id

                    FocusCard(
                        onClick = {
                            if (isExpanded) onInspectSession(null)
                            else onInspectSession(session)
                        }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Title, Date, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FocusTheme.colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = formatDateTime(session.startTimestamp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusTheme.colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                val statusColor = when (session.sessionStatus) {
                                    SessionStatus.COMPLETED -> FocusTheme.colors.success
                                    SessionStatus.STOPPED -> FocusTheme.colors.warning
                                    SessionStatus.ACTIVE -> FocusTheme.colors.primaryAccent
                                    else -> FocusTheme.colors.textSecondary
                                }

                                Text(
                                    text = session.sessionStatus.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metrics pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MiniMetricPill(
                                    label = "Focused",
                                    value = LiveTimerSnapshot.formatDuration(session.focusedDurationSeconds),
                                    color = FocusTheme.colors.primaryAccent
                                )

                                if (session.targetDurationSeconds > 0) {
                                    MiniMetricPill(
                                        label = "Target",
                                        value = LiveTimerSnapshot.formatDuration(session.targetDurationSeconds),
                                        color = FocusTheme.colors.textSecondary
                                    )
                                }

                                MiniMetricPill(
                                    label = "Paused",
                                    value = LiveTimerSnapshot.formatDuration(session.pausedDurationSeconds),
                                    color = FocusTheme.colors.warning
                                )

                                MiniMetricPill(
                                    label = "Distractions",
                                    value = "${session.distractionCount}",
                                    color = if (session.distractionCount > 0) FocusTheme.colors.error else FocusTheme.colors.textSecondary
                                )
                            }

                            // Expanded details
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 14.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(FocusTheme.colors.border)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Timeline within expanded card
                                    FocusTimelineSection(
                                        events = inspectedEvents,
                                        pauses = inspectedPauses,
                                        usages = inspectedUsages
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action to delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { sessionToDelete = session.id },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = FocusTheme.colors.error
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Delete Session", fontSize = 12.sp)
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

    // Delete session confirmation dialog
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Focus Session?") },
            text = { Text("This session and its timeline records will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToDelete?.let { onDeleteSession(it) }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocusTheme.colors.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = FocusTheme.colors.surface
        )
    }
}

@Composable
private fun MiniMetricPill(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FocusTheme.colors.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 10.sp
                )
            )
        }
    }
}

private fun formatDateTime(millis: Long): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        ""
    }
}
