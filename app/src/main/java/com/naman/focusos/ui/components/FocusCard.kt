package com.naman.focusos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naman.focusos.ui.theme.ElectricBlue
import com.naman.focusos.ui.theme.FocusTheme
import com.naman.focusos.ui.theme.MintEmerald

@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    borderColor: Color = FocusTheme.colors.border,
    backgroundColor: Color = FocusTheme.colors.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(modifier = cardModifier) {
        content()
    }
}

@Composable
fun ProgressRing(
    progress: Float, // 0.0f to 1.0f
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 105.dp,
    strokeWidth: Dp = 9.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress_ring_anim"
    )

    val trackColor = if (FocusTheme.colors.isDark) Color(0xFF1E2840) else Color(0xFFE2E8F0)
    val textColor = FocusTheme.colors.textPrimary
    val subtextColor = FocusTheme.colors.textSecondary
    val primaryAccent = FocusTheme.colors.primaryAccent
    val successColor = FocusTheme.colors.success

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = size.toPx() - strokePx
            val topLeft = strokePx / 2f

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active gradient progress arc
            val gradient = Brush.sweepGradient(
                colors = listOf(primaryAccent, successColor, primaryAccent)
            )
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val percentage = (progress * 100).toInt()
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
            Text(
                text = "$completedCount/$totalCount",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = subtextColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FocusTheme.colors.primaryAccent,
    unfocusedBorderColor = FocusTheme.colors.border,
    focusedLabelColor = FocusTheme.colors.primaryAccent,
    unfocusedLabelColor = FocusTheme.colors.textSecondary,
    focusedTextColor = FocusTheme.colors.textPrimary,
    unfocusedTextColor = FocusTheme.colors.textPrimary,
    cursorColor = FocusTheme.colors.primaryAccent,
    focusedContainerColor = FocusTheme.colors.surface,
    unfocusedContainerColor = FocusTheme.colors.surface
)

@Composable
fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = if (FocusTheme.colors.isDark) Color.Black else Color.White,
    checkedTrackColor = FocusTheme.colors.primaryAccent,
    uncheckedThumbColor = FocusTheme.colors.textSecondary,
    uncheckedTrackColor = FocusTheme.colors.surfaceVariant
)
