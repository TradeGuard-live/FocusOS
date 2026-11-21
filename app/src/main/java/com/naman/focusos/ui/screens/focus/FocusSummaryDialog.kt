package com.naman.focusos.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.ui.components.switchColors
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun FocusSummaryDialog(
    session: FocusSessionEntity?,
    snapshot: LiveTimerSnapshot,
    linkedTask: TaskEntity?,
    onDismiss: (markLinkedTaskCompleted: Boolean) -> Unit
) {
    var markTaskCompleted by remember { mutableStateOf(linkedTask != null) }

    val targetDurationFormatted = if (snapshot.targetDurationSeconds > 0) {
        LiveTimerSnapshot.formatDuration(snapshot.targetDurationSeconds)
    } else "Open-ended"

    val completionPercentage = snapshot.focusCompletionPercentage
    val coveragePercentage = if (snapshot.totalElapsedSeconds > 0) {
        (((snapshot.focusedSeconds + snapshot.pausedSeconds + snapshot.distractedSeconds).toDouble() / snapshot.totalElapsedSeconds.toDouble()) * 100).toInt().coerceAtMost(100)
    } else 100

    AlertDialog(
        onDismissRequest = { onDismiss(markTaskCompleted) },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FocusTheme.colors.primaryAccentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Session Complete!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = session?.title ?: "Focus Work Summary",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus Score Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completionPercentage%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FocusTheme.colors.primaryAccent
                                )
                            )
                            Text(
                                text = "Focus Score",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(FocusTheme.colors.border)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = snapshot.formattedFocusedTime,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FocusTheme.colors.success
                                )
                            )
                            Text(
                                text = "Actual Focused",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Metrics Breakdown Rows
                SummaryMetricRow("Target Planned", targetDurationFormatted)
                SummaryMetricRow("Total Session Elapsed", snapshot.formattedTotalElapsed)
                SummaryMetricRow("Actual Focused Time", snapshot.formattedFocusedTime)
                SummaryMetricRow("Total Paused Time", snapshot.formattedPausedTime)
                SummaryMetricRow("Distraction Time", LiveTimerSnapshot.formatDuration(snapshot.distractedSeconds))
                SummaryMetricRow("Productive Outside Time", LiveTimerSnapshot.formatDuration(snapshot.productiveSeconds))
                SummaryMetricRow("Distractions Detected", "${snapshot.distractionCount}")
                SummaryMetricRow("App Switches", "${snapshot.appSwitchCount}")
                SummaryMetricRow("Monitoring Coverage", "$coveragePercentage%")

                // Mark linked task as completed option
                if (linkedTask != null) {
                    HorizontalDivider(
                        color = FocusTheme.colors.border,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusTheme.colors.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = FocusTheme.colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mark task completed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.textPrimary
                                )
                            )
                            Text(
                                text = linkedTask.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusTheme.colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Switch(
                            checked = markTaskCompleted,
                            onCheckedChange = { markTaskCompleted = it },
                            colors = switchColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss(markTaskCompleted) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primaryAccent,
                    contentColor = FocusTheme.colors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DONE & SAVE", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = FocusTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SummaryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = FocusTheme.colors.textSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = FocusTheme.colors.textPrimary
            )
        )
    }
}
