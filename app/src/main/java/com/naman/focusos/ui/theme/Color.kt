package com.naman.focusos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Futuristic Dark Palette (Preserves Focus OS Identity)
// ==========================================
val DarkBg = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF141C2E)
val DarkSurfaceVariant = Color(0xFF1E2840)
val DarkBorder = Color(0xFF263554)

// Vibrant Futuristic Accents
val ElectricBlue = Color(0xFF19B9EF)
val NeonCyan = Color(0xFF06B6D4)
val VividViolet = Color(0xFF8B5CF6)
val MintEmerald = Color(0xFF10B981)
val WarmAmber = Color(0xFFF59E0B)
val CrimsonCoral = Color(0xFFEF4444)
val CyberPink = Color(0xFFEC4899)

// Text Colors (Dark Mode)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// ==========================================
// Apple-Inspired Light Palette (Clean & Minimal)
// ==========================================
val LightBg = Color(0xFFF5F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDF2F7)
val LightBorder = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF172033)
val TextSecondaryLight = Color(0xFF697586)
val TextMutedLight = Color(0xFF94A3B8)

@Immutable
data class FocusColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean
) {
    val danger: Color get() = error
}

val DarkFocusColors = FocusColors(
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    border = DarkBorder,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    primaryAccent = ElectricBlue,
    success = MintEmerald,
    warning = WarmAmber,
    error = CrimsonCoral,
    isDark = true
)

val LightFocusColors = FocusColors(
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    border = LightBorder,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    primaryAccent = Color(0xFF19B9EF),
    success = MintEmerald,
    warning = WarmAmber,
    error = CrimsonCoral,
    isDark = false
)

val LocalFocusColors = staticCompositionLocalOf { DarkFocusColors }
