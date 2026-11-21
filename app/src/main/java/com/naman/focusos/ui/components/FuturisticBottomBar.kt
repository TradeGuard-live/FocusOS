package com.naman.focusos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.ui.theme.FocusTheme

enum class FocusScreen(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.CheckCircle),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    FOCUS("Focus", Icons.Default.HourglassEmpty),
    ROUTINES("Routines", Icons.Default.Repeat),
    ANALYTICS("Analytics", Icons.Default.Insights),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun FuturisticBottomBar(
    currentScreen: FocusScreen,
    onScreenSelected: (FocusScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FocusTheme.colors.surface.copy(alpha = 0.95f))
            .border(1.dp, FocusTheme.colors.border, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FocusScreen.entries.forEach { screen ->
                val isSelected = screen == currentScreen

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) FocusTheme.colors.primaryAccent else FocusTheme.colors.textSecondary,
                    animationSpec = tween(250),
                    label = "nav_icon_color"
                )

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(200),
                    label = "nav_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onScreenSelected(screen) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = iconColor,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scale)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = iconColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.5.sp
                        )
                    )

                    // Active glow dot
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(FocusTheme.colors.primaryAccent)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
