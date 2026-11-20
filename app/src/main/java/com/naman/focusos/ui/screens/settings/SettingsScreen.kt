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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.naman.focusos.ui.theme.FocusTheme

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

            // 1. Appearance & RupeeFlow 4-Theme System
            item {
                SettingsSection(title = "THEME & AESTHETICS", icon = Icons.Default.Palette) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select your preferred theme. Changes take effect instantly across all screens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FocusTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )

                        // 1. Peachy Cozy (Active / Light Default)
                        val isPeachy = settings.themeMode == "PEACHY" || settings.themeMode == "LIGHT"
                        ThemePreviewCard(
                            title = "Peachy Cozy",
                            subtitle = "Warm, cute, minimal and modern peach aesthetic",
                            paletteColors = listOf(Color(0xFFFFF8F1), Color(0xFFFFAD91), Color(0xFFF58B72), Color(0xFF268B65), Color(0xFF4A3028)),
                            sampleBackground = Color(0xFFFFF0E6),
                            sampleBorder = Color(0xFFF0DDD2),
                            sampleTextPrimary = Color(0xFF4A3028),
                            sampleTextSecondary = Color(0xFF92766B),
                            sampleAccent = Color(0xFFF58B72),
                            sampleTagColor = Color(0xFF268B65),
                            isSelected = isPeachy,
                            onClick = { viewModel.setThemeMode("PEACHY") }
                        )

                        // 2. Lavender Dream
                        val isLavender = settings.themeMode == "LAVENDER"
                        ThemePreviewCard(
                            title = "Lavender Dream",
                            subtitle = "Soft lavender, pastel purple, lilac & tranquil serenity",
                            paletteColors = listOf(Color(0xFFF7F3FC), Color(0xFF9274C8), Color(0xFFB9A4E8), Color(0xFFE6BCEB), Color(0xFF342A45)),
                            sampleBackground = Color(0xFFEEE7F7),
                            sampleBorder = Color(0xFFE2D8EF),
                            sampleTextPrimary = Color(0xFF342A45),
                            sampleTextSecondary = Color(0xFF80748F),
                            sampleAccent = Color(0xFF9274C8),
                            sampleTagColor = Color(0xFF269B78),
                            isSelected = isLavender,
                            onClick = { viewModel.setThemeMode("LAVENDER") }
                        )

                        // 3. Midnight Mochi
                        val isMidnightMochi = settings.themeMode == "MIDNIGHT_MOCHI"
                        ThemePreviewCard(
                            title = "Midnight Mochi",
                            subtitle = "Deep midnight purple, dark plum surfaces & lavender glow",
                            paletteColors = listOf(Color(0xFF100E19), Color(0xFF191626), Color(0xFFB49AEF), Color(0xFFF0A9D5), Color(0xFFF5F0FC)),
                            sampleBackground = Color(0xFF211D31),
                            sampleBorder = Color(0xFF393149),
                            sampleTextPrimary = Color(0xFFF5F0FC),
                            sampleTextSecondary = Color(0xFFB6ADC9),
                            sampleAccent = Color(0xFFB49AEF),
                            sampleTagColor = Color(0xFF55C99B),
                            isSelected = isMidnightMochi,
                            onClick = { viewModel.setThemeMode("MIDNIGHT_MOCHI") }
                        )

                        // 4. Midnight Dark (Original RupeeFlow Theme)
                        val isDark = settings.themeMode == "DARK"
                        ThemePreviewCard(
                            title = "Midnight Dark",
                            subtitle = "High-contrast dark mode with electric blue neon accents",
                            paletteColors = listOf(Color(0xFF0A0E17), Color(0xFF111827), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF8FAFC)),
                            sampleBackground = Color(0xFF1E293B),
                            sampleBorder = Color(0x3394A3B8),
                            sampleTextPrimary = Color(0xFFF8FAFC),
                            sampleTextSecondary = Color(0xFF94A3B8),
                            sampleAccent = Color(0xFF3B82F6),
                            sampleTagColor = Color(0xFF10B981),
                            isSelected = isDark,
                            onClick = { viewModel.setThemeMode("DARK") }
                        )

                        // 5. System Default
                        val isSystem = settings.themeMode == "SYSTEM"
                        ThemePreviewCard(
                            title = "System Default",
                            subtitle = "Automatically follows Android system light / dark appearance",
                            paletteColors = listOf(Color(0xFF172033), Color(0xFF697586), Color(0xFFE2E8F0), Color(0xFF0A0E17), Color(0xFF3B82F6)),
                            sampleBackground = FocusTheme.colors.surfaceVariant,
                            sampleBorder = FocusTheme.colors.border,
                            sampleTextPrimary = FocusTheme.colors.textPrimary,
                            sampleTextSecondary = FocusTheme.colors.textSecondary,
                            sampleAccent = FocusTheme.colors.primaryAccent,
                            sampleTagColor = FocusTheme.colors.success,
                            isSelected = isSystem,
                            onClick = { viewModel.setThemeMode("SYSTEM") }
                        )
                    }
                }
            }

            // 2. Locale & Calendar
            item {
                SettingsSection(title = "LOCALE & CALENDAR", icon = Icons.Default.Schedule) {
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

            // 3. Focus Timing Defaults
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

            // 4. Notifications & Reminders
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
fun ThemePreviewCard(
    title: String,
    subtitle: String,
    paletteColors: List<Color>,
    sampleBackground: Color,
    sampleBorder: Color,
    sampleTextPrimary: Color,
    sampleTextSecondary: Color,
    sampleAccent: Color,
    sampleTagColor: Color = sampleAccent,
    isSelected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.border

    FocusCard(
        cornerRadius = 16.dp,
        borderColor = borderColor,
        backgroundColor = FocusTheme.colors.surface,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title, Active Badge, Radio Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FocusTheme.colors.textPrimary
                    )

                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FocusTheme.colors.surfaceVariant)
                                .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTheme.colors.textSecondary
                            )
                        }
                    } else if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FocusTheme.colors.primaryAccentContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = FocusTheme.colors.primaryAccent
                                )
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.primaryAccent
                                )
                            }
                        }
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    enabled = badgeText == null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = FocusTheme.colors.primaryAccent,
                        unselectedColor = FocusTheme.colors.textSecondary
                    )
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = FocusTheme.colors.textSecondary
            )

            // Color swatch dots preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                paletteColors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, FocusTheme.colors.border, CircleShape)
                    )
                }
            }

            // Realistic Mini Focus Task Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(sampleBackground)
                    .border(1.dp, sampleBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(sampleAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = sampleAccent
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Deep Work Session",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = sampleTextPrimary
                            )
                            Text(
                                text = "Focus OS • 09:00 – 10:30",
                                style = MaterialTheme.typography.labelSmall,
                                color = sampleTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Mini tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(sampleTagColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRIORITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = sampleTagColor
                        )
                    }
                }
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
