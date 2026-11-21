package com.naman.focusos.ui.screens.focus

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.SessionStatus
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun FocusWorkScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermission()
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FOCUS WORK ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Deep Focus",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                }

                // Privacy / Usage Permission icon button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surfaceVariant)
                        .clickable { viewModel.showPermissionOnboarding(true) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy & Usage Access",
                            tint = if (uiState.hasUsagePermission) FocusTheme.colors.success else FocusTheme.colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (uiState.hasUsagePermission) "Monitored" else "Setup Access",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.hasUsagePermission) FocusTheme.colors.success else FocusTheme.colors.warning,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Tabs: Timer, History, Analytics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FocusTheme.colors.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FocusTab.entries.forEach { tab ->
                    val isSelected = tab == uiState.selectedTab
                    val tabIcon = when (tab) {
                        FocusTab.TIMER -> Icons.Default.Timer
                        FocusTab.HISTORY -> Icons.Default.History
                        FocusTab.ANALYTICS -> Icons.Default.Insights
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) FocusTheme.colors.surface
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) FocusTheme.colors.border else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            Crossfade(
                targetState = uiState.selectedTab,
                label = "focus_tab_crossfade"
            ) { tab ->
                when (tab) {
                    FocusTab.TIMER -> {
                        val isSessionActive = uiState.liveSnapshot.status == SessionStatus.ACTIVE ||
                                uiState.liveSnapshot.status == SessionStatus.PAUSED ||
                                (uiState.activeSession != null && uiState.activeSession?.sessionStatus != SessionStatus.COMPLETED && uiState.activeSession?.sessionStatus != SessionStatus.STOPPED)

                        if (isSessionActive) {
                            val linkedTask = uiState.availableTasks.find { it.id == uiState.activeSession?.linkedTaskId }
                            ActiveFocusScreen(
                                session = uiState.activeSession,
                                snapshot = uiState.liveSnapshot,
                                linkedTask = linkedTask,
                                pauses = uiState.activeSessionPauses,
                                usages = uiState.activeSessionUsages,
                                events = uiState.activeSessionEvents,
                                hasUsagePermission = uiState.hasUsagePermission,
                                showStopConfirmation = uiState.showStopConfirmation,
                                onPause = { viewModel.pauseSession() },
                                onResume = { viewModel.resumeSession() },
                                onRequestStop = { viewModel.requestStopSession() },
                                onConfirmStop = { viewModel.confirmStopSession() },
                                onDismissStop = { viewModel.dismissStopConfirmation() },
                                onComplete = { viewModel.completeSession() }
                            )
                        } else {
                            FocusSetupView(
                                availableTasks = uiState.availableTasks,
                                availableCategories = uiState.availableCategories,
                                hasUsagePermission = uiState.hasUsagePermission,
                                defaultDurationMinutes = uiState.defaultDurationMinutes,
                                onStartSession = { title, desc, taskId, cat, mins, monitor ->
                                    viewModel.startSession(title, desc, taskId, cat, mins, monitor)
                                },
                                onRequestUsagePermission = {
                                    viewModel.showPermissionOnboarding(true)
                                }
                            )
                        }
                    }

                    FocusTab.HISTORY -> {
                        FocusHistoryView(
                            sessions = uiState.filteredSessions,
                            activeFilter = uiState.historyFilter,
                            inspectedSession = uiState.inspectedSession,
                            inspectedPauses = uiState.inspectedSessionPauses,
                            inspectedUsages = uiState.inspectedSessionUsages,
                            inspectedEvents = uiState.inspectedSessionEvents,
                            onFilterSelected = { viewModel.setHistoryFilter(it) },
                            onInspectSession = { viewModel.inspectSession(it) },
                            onDeleteSession = { viewModel.deleteSession(it) }
                        )
                    }

                    FocusTab.ANALYTICS -> {
                        FocusAnalyticsView(
                            uiState = uiState,
                            onFilterSelected = { viewModel.setAnalyticsDateFilter(it) },
                            onSelectHeatmapCell = { viewModel.selectHeatmapCell(it) }
                        )
                    }
                }
            }
        }
    }

    // Completion Report Summary Dialog
    if (uiState.showCompletionDialog && uiState.completionReportSnapshot != null) {
        val linkedTask = uiState.availableTasks.find { it.id == uiState.activeSession?.linkedTaskId }
        FocusSummaryDialog(
            session = uiState.activeSession,
            snapshot = uiState.completionReportSnapshot!!,
            linkedTask = linkedTask,
            onDismiss = { markCompleted ->
                viewModel.dismissCompletionDialog(markCompleted)
            }
        )
    }

    // Permission Onboarding Modal
    if (uiState.showPermissionOnboarding) {
        FocusPermissionOnboardingDialog(
            onDismiss = {
                viewModel.showPermissionOnboarding(false)
                viewModel.checkPermission()
            }
        )
    }
}
