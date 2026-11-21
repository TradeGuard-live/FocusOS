package com.naman.focusos.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.data.model.AppClassification
import com.naman.focusos.data.model.AppClassificationEntity
import com.naman.focusos.ui.theme.FocusTheme

@Composable
fun AppClassificationDialog(
    classifications: List<AppClassificationEntity>,
    onUpdateClassification: (packageName: String, appName: String, classification: AppClassification) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = FocusTheme.colors.primaryAccent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "App Distraction Categories",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Text(
                    text = "Configure how Focus OS classifies external applications during your focus sessions.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FocusTheme.colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(classifications, key = { it.packageName }) { app ->
                        AppClassificationRow(
                            app = app,
                            onUpdate = { newClassification ->
                                onUpdateClassification(app.packageName, app.applicationName, newClassification)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = FocusTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AppClassificationRow(
    app: AppClassificationEntity,
    onUpdate: (AppClassification) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val badgeColor = when (app.classification) {
        AppClassification.DISTRACTING_APP -> FocusTheme.colors.error
        AppClassification.PRODUCTIVE_APP -> FocusTheme.colors.success
        AppClassification.FOCUS_APP -> FocusTheme.colors.primaryAccent
        AppClassification.SYSTEM_APP -> FocusTheme.colors.textSecondary
        else -> FocusTheme.colors.warning
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FocusTheme.colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.applicationName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    )
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.textSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = app.classification.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        AppClassification.PRODUCTIVE_APP,
                        AppClassification.DISTRACTING_APP,
                        AppClassification.NEUTRAL_APP,
                        AppClassification.SYSTEM_APP
                    ).forEach { classification ->
                        DropdownMenuItem(
                            text = { Text(classification.displayName) },
                            onClick = {
                                onUpdate(classification)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
