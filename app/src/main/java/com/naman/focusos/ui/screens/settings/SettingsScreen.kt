package com.naman.focusos.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.ui.components.FocusCard
import com.naman.focusos.ui.components.switchColors
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocusTheme.colors.primaryAccent,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusTheme.colors.textPrimary
                        )
                    )
                }
            }

            // Status message banner
            statusMsg?.let { msg ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (FocusTheme.colors.isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = msg, color = if (FocusTheme.colors.isDark) Color.White else Color(0xFF065F46), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = "Dismiss",
                                color = FocusTheme.colors.success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { viewModel.clearStatusMessage() }
                            )
                        }
                    }
                }
            }

            // 1. Appearance & Locale
            item {
                SettingsSection(title = "APPEARANCE & LOCALE", icon = Icons.Default.Palette) {
                    SettingOptionSelector(
                        title = "Theme",
                        options = listOf(
                            "DARK" to "🌙 Dark",
                            "LIGHT" to "☀️ Light",
                            "SYSTEM" to "📱 System Default"
                        ),
                        selected = settings.themeMode,
                        onSelect = { viewModel.setThemeMode(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingOptionSelector(
                        title = "Time Format",
                        options = listOf("12H" to "12-Hour (AM/PM)", "24H" to "24-Hour (Railway)"),
                        selected = settings.timeFormat,
                        onSelect = { viewModel.setTimeFormat(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingOptionSelector(
                        title = "First Day of Week",
                        options = listOf("MONDAY" to "Monday", "SUNDAY" to "Sunday"),
                        selected = settings.firstDayOfWeek,
                        onSelect = { viewModel.setFirstDayOfWeek(it) }
                    )
                }
            }

            // 2. Focus Timing Defaults
            item {
                SettingsSection(title = "FOCUS TIMING DEFAULTS", icon = Icons.Default.Timer) {
                    SettingOptionSelector(
                        title = "Default Task Duration",
                        options = listOf(15 to "15m", 30 to "30m", 60 to "60m", 90 to "90m"),
                        selected = settings.defaultDurationMinutes,
                        onSelect = { viewModel.setDefaultDuration(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingOptionSelector(
                        title = "Advance Reminder",
                        options = listOf(0 to "At Start", 5 to "5m Before", 10 to "10m Before", 15 to "15m Before"),
                        selected = settings.defaultReminderMinutes,
                        onSelect = { viewModel.setDefaultReminder(it) }
                    )
                }
            }

            // 3. Notifications & Reminders
            item {
                SettingsSection(title = "NOTIFICATIONS & REMINDERS", icon = Icons.Default.Notifications) {
                    SettingSwitchRow(
                        title = "Enable Notifications",
                        subtitle = "High-priority start & deadline alerts",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Vibration Alert",
                        subtitle = "Tactile alert on reminder trigger",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Sound Alert",
                        subtitle = "Audio prompt for scheduled tasks",
                        checked = settings.soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    FocusCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = FocusTheme.colors.primaryAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusTheme.colors.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun <T> SettingOptionSelector(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = FocusTheme.colors.textPrimary, fontWeight = FontWeight.Medium))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.surfaceVariant)
                        .border(1.dp, if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border, RoundedCornerShape(8.dp))
                        .clickable { onSelect(value) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) (if (FocusTheme.colors.isDark) Color.Black else Color.White) else FocusTheme.colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = FocusTheme.colors.textPrimary, fontWeight = FontWeight.Medium))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = FocusTheme.colors.textSecondary, fontSize = 11.sp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = switchColors())
    }
}
