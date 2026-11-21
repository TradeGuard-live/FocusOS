package com.naman.focusos.ui.screens.focus

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.domain.focus.AndroidAppUsageProvider
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun FocusPermissionOnboardingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(FocusTheme.colors.primaryAccentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = FocusTheme.colors.primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Distraction Monitoring & Privacy",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Allow Focus OS to identify which applications you open during your focus sessions. This helps calculate distractions and improve your productivity reports.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textPrimary,
                        lineHeight = 18.sp
                    )
                )

                PrivacyPoint(
                    icon = Icons.Default.Lock,
                    title = "100% Local & Private",
                    description = "Usage stats are processed on device and stored in your local Room database. Never uploaded to any server."
                )

                PrivacyPoint(
                    icon = Icons.Default.Security,
                    title = "Session-Only Tracking",
                    description = "Focus OS only checks usage while a session is active or paused. Monitoring stops immediately when you end the session."
                )

                PrivacyPoint(
                    icon = Icons.Default.VisibilityOff,
                    title = "Zero Content Access",
                    description = "Focus OS only inspects foreground app names. No keystrokes, messages, screenshots, or browsing history are ever accessed."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        context.startActivity(AndroidAppUsageProvider.createUsageAccessIntent())
                    } catch (e: Exception) {
                        // In case intent fails on some OEMs
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primaryAccent,
                    contentColor = FocusTheme.colors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Android Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Maybe Later")
            }
        },
        containerColor = FocusTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PrivacyPoint(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(FocusTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FocusTheme.colors.primaryAccent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FocusTheme.colors.textPrimary
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = FocusTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}
