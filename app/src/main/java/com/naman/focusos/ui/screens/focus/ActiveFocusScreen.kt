package com.naman.focusos.ui.screens.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.data.model.TaskEntity
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun ActiveFocusScreen(
    session: FocusSessionEntity?,
    snapshot: LiveTimerSnapshot,
    linkedTask: TaskEntity?,
    pauses: List<FocusPauseEntity>,
    usages: List<FocusAppUsageEntity>,
    events: List<FocusSessionEventEntity>,
    hasUsagePermission: Boolean,
    showStopConfirmation: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestStop: () -> Unit,
    onConfirmStop: () -> Unit,
    onDismissStop: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = snapshot.status == SessionStatus.PAUSED
    val isTargetSet = snapshot.targetDurationSeconds > 0
    val targetDurationFormatted = if (isTargetSet) LiveTimerSnapshot.formatDuration(snapshot.targetDurationSeconds) else "Open"

    val animatedProgress by animateFloatAsState(
        targetValue = if (isTargetSet) snapshot.progress else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "timer_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Status & Session Header
        FocusCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPaused) FocusTheme.colors.warningContainer
                            else FocusTheme.colors.successContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) FocusTheme.colors.warning
                                else FocusTheme.colors.success
                            )
                    )
                    Text(
                        text = if (isPaused) "SESSION PAUSED" else "FOCUSING ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isPaused) FocusTheme.colors.warning else FocusTheme.colors.success,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = session?.title ?: "Focus Session",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                if (linkedTask != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = FocusTheme.colors.primaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Linked: ${linkedTask.title}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocusTheme.colors.primaryAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Circular Giant Timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .padding(8.dp)
        ) {
            val progressColor = if (isPaused) FocusTheme.colors.warning else FocusTheme.colors.primaryAccent
            val trackColor = FocusTheme.colors.surfaceVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )
                // Progress
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            progressColor.copy(alpha = 0.6f),
                            progressColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isPaused) snapshot.formattedPausedTime else snapshot.formattedFocusedTime,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp,
                        letterSpacing = 1.sp,
                        color = if (isPaused) FocusTheme.colors.warning else FocusTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPaused) "Paused Time" else {
                        if (isTargetSet) "Target: $targetDurationFormatted (${(snapshot.progress * 100).toInt()}%)"
                        else "Elapsed: ${snapshot.formattedTotalElapsed}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (isTargetSet && !isPaused) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Remaining: ${snapshot.formattedRemainingTime}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Metrics Grid (Focused, Paused, Distractions, App Switches)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FocusMetricTile(
                label = "Focused",
                value = snapshot.formattedFocusedTime,
                color = FocusTheme.colors.primaryAccent,
                modifier = Modifier.weight(1f)
            )

            FocusMetricTile(
                label = "Paused",
                value = snapshot.formattedPausedTime,
                color = FocusTheme.colors.warning,
                modifier = Modifier.weight(1f)
            )

            FocusMetricTile(
                label = "Distractions",
                value = "${snapshot.distractionCount}",
                color = FocusTheme.colors.error,
                modifier = Modifier.weight(1f)
            )

            FocusMetricTile(
                label = "Switches",
                value = "${snapshot.appSwitchCount}",
                color = FocusTheme.colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        // Monitoring Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = if (hasUsagePermission) FocusTheme.colors.success else FocusTheme.colors.warning,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasUsagePermission) "Distraction monitoring active" else "Monitoring limited (Usage access not granted)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (hasUsagePermission) FocusTheme.colors.success else FocusTheme.colors.warning,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pause / Resume Button
            Button(
                onClick = { if (isPaused) onResume() else onPause() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) FocusTheme.colors.success else FocusTheme.colors.warningContainer,
                    contentColor = if (isPaused) Color.White else FocusTheme.colors.warning
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPaused) "RESUME" else "PAUSE",
                    fontWeight = FontWeight.Bold
                )
            }

            // Complete Button
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primaryAccent,
                    contentColor = FocusTheme.colors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COMPLETE",
                    fontWeight = FontWeight.Bold
                )
            }

            // Stop Button
            OutlinedButton(
                onClick = onRequestStop,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FocusTheme.colors.error
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Live Chronological Timeline
        FocusTimelineSection(
            events = events,
            pauses = pauses,
            usages = usages
        )
    }

    // Stop Confirmation Dialog
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissStop,
            title = {
                Text(
                    text = "End Focus Session?",
                    fontWeight = FontWeight.Bold,
                    color = FocusTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop this session? Your focused time and analytics will be saved to your history.",
                    color = FocusTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocusTheme.colors.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("End Session", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissStop) {
                    Text("Keep Focusing")
                }
            },
            containerColor = FocusTheme.colors.surface
        )
    }
}

@Composable
fun FocusMetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FocusTheme.colors.surface)
            .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}
