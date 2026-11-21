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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.FocusAppUsageEntity
import com.naman.focusos.data.model.FocusPauseEntity
import com.naman.focusos.data.model.FocusSessionEventEntity
import com.naman.focusos.data.model.SessionEventType
import com.naman.focusos.domain.focus.LiveTimerSnapshot
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.theme.FocusTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class UnifiedTimelineItem(
    val timestamp: Long,
    val title: String,
    val subtitle: String?,
    val durationSeconds: Long?,
    val icon: ImageVector,
    val iconTint: Color,
    val classification: AppClassification?
)

@Composable
fun FocusTimelineSection(
    events: List<FocusSessionEventEntity>,
    pauses: List<FocusPauseEntity>,
    usages: List<FocusAppUsageEntity>,
    modifier: Modifier = Modifier
) {
    val items = buildUnifiedTimeline(events, pauses, usages)

    FocusCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = FocusTheme.colors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LIVE SESSION TIMELINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (items.isEmpty()) {
                Text(
                    text = "Session in progress... Timeline events will appear here.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textSecondary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEachIndexed { index, item ->
                        TimelineRow(
                            item = item,
                            isLast = index == items.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineRow(
    item: UnifiedTimelineItem,
    isLast: Boolean
) {
    val timeFormatted = formatTime(item.timestamp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = FocusTheme.colors.textSecondary,
                fontSize = 11.sp
            ),
            modifier = Modifier.width(55.dp)
        )

        // Line and icon dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(item.iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(FocusTheme.colors.border)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    )
                )

                if (item.classification != null) {
                    val badgeColor = when (item.classification) {
                        AppClassification.DISTRACTING_APP -> FocusTheme.colors.error
                        AppClassification.PRODUCTIVE_APP -> FocusTheme.colors.success
                        AppClassification.PAUSED_SESSION_USAGE -> FocusTheme.colors.warning
                        else -> FocusTheme.colors.textSecondary
                    }
                    Text(
                        text = item.classification.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            if (item.durationSeconds != null && item.durationSeconds > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Duration: ${LiveTimerSnapshot.formatDuration(item.durationSeconds)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

private fun buildUnifiedTimeline(
    events: List<FocusSessionEventEntity>,
    pauses: List<FocusPauseEntity>,
    usages: List<FocusAppUsageEntity>
): List<UnifiedTimelineItem> {
    val items = mutableListOf<UnifiedTimelineItem>()

    for (e in events) {
        val (icon, tint) = when (e.eventType) {
            SessionEventType.SESSION_STARTED -> Icons.Default.PlayCircle to Color(0xFF10B981)
            SessionEventType.SESSION_PAUSED -> Icons.Default.PauseCircle to Color(0xFFF59E0B)
            SessionEventType.SESSION_RESUMED -> Icons.Default.PlayCircle to Color(0xFF3B82F6)
            SessionEventType.SESSION_STOPPED -> Icons.Default.StopCircle to Color(0xFFEF4444)
            SessionEventType.SESSION_COMPLETED -> Icons.Default.CheckCircle to Color(0xFF10B981)
            SessionEventType.APP_OPENED -> Icons.AutoMirrored.Filled.OpenInNew to Color(0xFFF97316)
            SessionEventType.RETURNED_TO_FOCUS -> Icons.Default.CheckCircle to Color(0xFF06B6D4)
            SessionEventType.MONITORING_INTERRUPTED -> Icons.Default.Warning to Color(0xFFEF4444)
            SessionEventType.MONITORING_RESTORED -> Icons.Default.Info to Color(0xFF10B981)
            SessionEventType.APP_CLOSED -> Icons.Default.Info to Color(0xFF6B7280)
        }

        val title = when (e.eventType) {
            SessionEventType.SESSION_STARTED -> "Focus session started"
            SessionEventType.SESSION_PAUSED -> "Session paused"
            SessionEventType.SESSION_RESUMED -> "Session resumed"
            SessionEventType.SESSION_STOPPED -> "Session stopped"
            SessionEventType.SESSION_COMPLETED -> "Session completed"
            SessionEventType.APP_OPENED -> "Opened ${e.relatedAppPackage?.substringAfterLast('.') ?: "App"}"
            SessionEventType.RETURNED_TO_FOCUS -> "Returned to Focus OS"
            SessionEventType.MONITORING_INTERRUPTED -> "Monitoring interrupted"
            SessionEventType.MONITORING_RESTORED -> "Monitoring restored"
            SessionEventType.APP_CLOSED -> "App closed"
        }

        items.add(
            UnifiedTimelineItem(
                timestamp = e.eventTimestamp,
                title = title,
                subtitle = e.metadata,
                durationSeconds = null,
                icon = icon,
                iconTint = tint,
                classification = null
            )
        )
    }

    return items.sortedBy { it.timestamp }
}

private fun formatTime(millis: Long): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        ""
    }
}
