package com.naman.focusos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// RUPEEFLOW DESIGN SYSTEM — 4-THEME SEMANTIC COLOR ARCHITECTURE
// =============================================================================

@Immutable
data class FocusColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceSecondary: Color = surfaceVariant,
    val surfaceElevated: Color = surface,
    val border: Color,
    val cardBorder: Color = border,
    val divider: Color = border,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textTertiary: Color = textMuted,
    val primaryAccent: Color,
    val primaryAccentContainer: Color = surfaceVariant,
    val onPrimaryAccent: Color = Color.White,
    val success: Color,
    val successContainer: Color = success.copy(alpha = 0.2f),
    val onSuccess: Color = Color.White,
    val warning: Color,
    val warningContainer: Color = warning.copy(alpha = 0.2f),
    val onWarning: Color = Color.White,
    val error: Color,
    val errorContainer: Color = error.copy(alpha = 0.2f),
    val onError: Color = Color.White,
    val info: Color = primaryAccent,
    val infoContainer: Color = surfaceVariant,
    val isDark: Boolean,
    val primaryPeach: Color = Color(0xFFFFAD91),
    val deepPeach: Color = Color(0xFFF58B72),
    val accentPink: Color = Color(0xFFF5A6B8),
    val softLavender: Color = Color(0xFFDCCBFA),
    val mint: Color = Color(0xFFB9E5D2),
    val warmYellow: Color = Color(0xFFFFD98E)
) {
    val danger: Color get() = error
    val cardBackground: Color get() = surface
    val accentContainer: Color get() = primaryAccentContainer
    val inputBackground: Color get() = if (isDark) surfaceSecondary else surface
    val incomeGreen: Color get() = success
    val expenseRed: Color get() = error
}

typealias AppColors = FocusColors

// =============================================================================
// 1. PEACHY COZY DESIGN SYSTEM (Warm, Cute, Minimal & Modern)
// =============================================================================

val PeachyFocusColors = FocusColors(
    background = Color(0xFFFFF8F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFF0E6),
    surfaceSecondary = Color(0xFFFFF0E6),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFF0DDD2),
    cardBorder = Color(0xFFF0DDD2),
    divider = Color(0xFFF0DDD2),
    textPrimary = Color(0xFF4A3028),
    textSecondary = Color(0xFF92766B),
    textMuted = Color(0xFFB8A298),
    textTertiary = Color(0xFFB8A298),
    primaryAccent = Color(0xFFF58B72),
    primaryAccentContainer = Color(0xFFFFF0E6),
    onPrimaryAccent = Color(0xFFFFFFFF),
    success = Color(0xFF268B65),
    successContainer = Color(0xFFD8F3E5),
    onSuccess = Color(0xFFFFFFFF),
    error = Color(0xFFD95757),
    errorContainer = Color(0xFFFDE8E8),
    onError = Color(0xFFFFFFFF),
    warning = Color(0xFFFFD98E),
    warningContainer = Color(0xFFFFF5DC),
    onWarning = Color(0xFF4A3028),
    info = Color(0xFFDCCBFA),
    infoContainer = Color(0xFFF4EEFE),
    isDark = false,
    primaryPeach = Color(0xFFFFAD91),
    deepPeach = Color(0xFFF58B72),
    accentPink = Color(0xFFF5A6B8),
    softLavender = Color(0xFFDCCBFA),
    mint = Color(0xFFB9E5D2),
    warmYellow = Color(0xFFFFD98E)
)

val PeachyAppColors = PeachyFocusColors

// =============================================================================
// 2. LAVENDER DREAM DESIGN SYSTEM (Soft lavender, pastel purple, lilac & tranquil serenity)
// =============================================================================

val LavenderFocusColors = FocusColors(
    background = Color(0xFFF7F3FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEE7F7),
    surfaceSecondary = Color(0xFFEEE7F7),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFE2D8EF),
    cardBorder = Color(0xFFE2D8EF),
    divider = Color(0xFFE2D8EF),
    textPrimary = Color(0xFF342A45),
    textSecondary = Color(0xFF80748F),
    textMuted = Color(0xFFA69BB5),
    textTertiary = Color(0xFFA69BB5),
    primaryAccent = Color(0xFF9274C8),
    primaryAccentContainer = Color(0xFFEEE7F7),
    onPrimaryAccent = Color(0xFFFFFFFF),
    success = Color(0xFF269B78),
    successContainer = Color(0xFFD8F3E9),
    onSuccess = Color(0xFFFFFFFF),
    error = Color(0xFFD95770),
    errorContainer = Color(0xFFFCE8ED),
    onError = Color(0xFFFFFFFF),
    warning = Color(0xFFD99B49),
    warningContainer = Color(0xFFFCF3E5),
    onWarning = Color(0xFFFFFFFF),
    info = Color(0xFFB9A4E8),
    infoContainer = Color(0xFFF0EAFC),
    isDark = false,
    primaryPeach = Color(0xFFB9A4E8),
    deepPeach = Color(0xFF9274C8),
    accentPink = Color(0xFFE6BCEB),
    softLavender = Color(0xFFC9B9E8),
    mint = Color(0xFF269B78),
    warmYellow = Color(0xFFD99B49)
)

val LavenderAppColors = LavenderFocusColors

// =============================================================================
// 3. MIDNIGHT MOCHI DESIGN SYSTEM (Deep midnight purple, dark plum surfaces & lavender highlights)
// =============================================================================

val MidnightMochiFocusColors = FocusColors(
    background = Color(0xFF100E19),
    surface = Color(0xFF191626),
    surfaceVariant = Color(0xFF252036),
    surfaceSecondary = Color(0xFF252036),
    surfaceElevated = Color(0xFF211D31),
    border = Color(0xFF393149),
    cardBorder = Color(0xFF393149),
    divider = Color(0x33B6ADC9),
    textPrimary = Color(0xFFF5F0FC),
    textSecondary = Color(0xFFB6ADC9),
    textMuted = Color(0xFF867C99),
    textTertiary = Color(0xFF867C99),
    primaryAccent = Color(0xFFB49AEF),
    primaryAccentContainer = Color(0xFF252036),
    onPrimaryAccent = Color(0xFF100E19),
    success = Color(0xFF55C99B),
    successContainer = Color(0x2655C99B),
    onSuccess = Color(0xFF100E19),
    error = Color(0xFFF07891),
    errorContainer = Color(0x26F07891),
    onError = Color(0xFFF5F0FC),
    warning = Color(0xFFE7B66C),
    warningContainer = Color(0x26E7B66C),
    onWarning = Color(0xFF100E19),
    info = Color(0xFFD0BDF8),
    infoContainer = Color(0x26D0BDF8),
    isDark = true,
    primaryPeach = Color(0xFFD0BDF8),
    deepPeach = Color(0xFFB49AEF),
    accentPink = Color(0xFFF0A9D5),
    softLavender = Color(0xFF9576CB),
    mint = Color(0xFF55C99B),
    warmYellow = Color(0xFFE7B66C)
)

val MidnightMochiAppColors = MidnightMochiFocusColors

// =============================================================================
// 4. EXISTING RUPEEFLOW THEME (Midnight Dark Aesthetic with Electric Blue Accents)
// =============================================================================

val DarkFocusColors = FocusColors(
    background = Color(0xFF0A0E17),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1E293B),
    surfaceSecondary = Color(0xFF1E293B),
    surfaceElevated = Color(0xFF1E293B),
    border = Color(0x3394A3B8),
    cardBorder = Color(0x3394A3B8),
    divider = Color(0x1A94A3B8),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    textTertiary = Color(0xFF64748B),
    primaryAccent = Color(0xFF3B82F6),
    primaryAccentContainer = Color(0xFF1E293B),
    onPrimaryAccent = Color(0xFFFFFFFF),
    success = Color(0xFF10B981),
    successContainer = Color(0x2610B981),
    onSuccess = Color(0xFF0A0E17),
    error = Color(0xFFEF4444),
    errorContainer = Color(0x26EF4444),
    onError = Color(0xFFF8FAFC),
    warning = Color(0xFFF59E0B),
    warningContainer = Color(0x26F59E0B),
    onWarning = Color(0xFF0A0E17),
    info = Color(0xFF06B6D4),
    infoContainer = Color(0x2606B6D4),
    isDark = true,
    primaryPeach = Color(0xFF60A5FA),
    deepPeach = Color(0xFF3B82F6),
    accentPink = Color(0xFFEC4899),
    softLavender = Color(0xFF8B5CF6),
    mint = Color(0xFF10B981),
    warmYellow = Color(0xFFF59E0B)
)

val DarkAppColors = DarkFocusColors

// =============================================================================
// 5. LIGHT DESIGN SYSTEM (Clean Fintech Dashboard Aesthetic)
// =============================================================================

val LightFocusColors = FocusColors(
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceSecondary = Color(0xFFF1F5F9),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFE5EAF2),
    cardBorder = Color(0xFFE5EAF2),
    divider = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF172033),
    textSecondary = Color(0xFF64748B),
    textMuted = Color(0xFF94A3B8),
    textTertiary = Color(0xFF94A3B8),
    primaryAccent = Color(0xFF2563EB),
    primaryAccentContainer = Color(0xFFDBEAFE),
    onPrimaryAccent = Color(0xFFFFFFFF),
    success = Color(0xFF059669),
    successContainer = Color(0xFFD1FAE5),
    onSuccess = Color(0xFFFFFFFF),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color(0xFFFFFFFF),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    onWarning = Color(0xFFFFFFFF),
    info = Color(0xFF0891B2),
    infoContainer = Color(0xFFCFFAFE),
    isDark = false
)

val LightAppColors = LightFocusColors

val LocalFocusColors = staticCompositionLocalOf { DarkFocusColors }
val LocalAppColors = LocalFocusColors

// =============================================================================
// PEACHY COZY COLOR CONSTANTS
// =============================================================================

val PeachyBackground = Color(0xFFFFF8F1)
val PeachySurface = Color(0xFFFFFFFF)
val PeachySecondarySurface = Color(0xFFFFF0E6)
val PeachyPrimaryPeach = Color(0xFFFFAD91)
val PeachyDeepPeach = Color(0xFFF58B72)
val PeachyAccentPink = Color(0xFFF5A6B8)
val PeachySoftLavender = Color(0xFFDCCBFA)
val PeachyMint = Color(0xFFB9E5D2)
val PeachyWarmYellow = Color(0xFFFFD98E)
val PeachyTextPrimary = Color(0xFF4A3028)
val PeachyTextSecondary = Color(0xFF92766B)
val PeachyBorder = Color(0xFFF0DDD2)
val PeachyError = Color(0xFFD95757)
val PeachySuccess = Color(0xFF268B65)

// =============================================================================
// LAVENDER DREAM COLOR CONSTANTS
// =============================================================================

val LavenderPrimary = Color(0xFF9274C8)
val LavenderPrimaryLight = Color(0xFFB9A4E8)
val LavenderSecondary = Color(0xFFC9B9E8)
val LavenderAccent = Color(0xFFE6BCEB)
val LavenderBackground = Color(0xFFF7F3FC)
val LavenderSurface = Color(0xFFFFFFFF)
val LavenderSurfaceVariant = Color(0xFFEEE7F7)
val LavenderTextPrimary = Color(0xFF342A45)
val LavenderTextSecondary = Color(0xFF80748F)
val LavenderBorder = Color(0xFFE2D8EF)
val LavenderSuccess = Color(0xFF269B78)
val LavenderError = Color(0xFFD95770)
val LavenderWarning = Color(0xFFD99B49)

// =============================================================================
// MIDNIGHT MOCHI COLOR CONSTANTS
// =============================================================================

val MidnightMochiPrimary = Color(0xFFB49AEF)
val MidnightMochiPrimaryLight = Color(0xFFD0BDF8)
val MidnightMochiSecondary = Color(0xFF9576CB)
val MidnightMochiAccent = Color(0xFFF0A9D5)
val MidnightMochiBackground = Color(0xFF100E19)
val MidnightMochiSurface = Color(0xFF191626)
val MidnightMochiSurfaceVariant = Color(0xFF252036)
val MidnightMochiSurfaceElevated = Color(0xFF211D31)
val MidnightMochiTextPrimary = Color(0xFFF5F0FC)
val MidnightMochiTextSecondary = Color(0xFFB6ADC9)
val MidnightMochiBorder = Color(0xFF393149)
val MidnightMochiSuccess = Color(0xFF55C99B)
val MidnightMochiError = Color(0xFFF07891)
val MidnightMochiWarning = Color(0xFFE7B66C)

// =============================================================================
// LEGACY & FOCUS OS CONSTANTS PRESERVED FOR DIRECT REFERENCES
// =============================================================================

val DarkBg = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkBorder = Color(0x3394A3B8)

val MidnightDark = Color(0xFF0A0E17)
val CharcoalSurface = Color(0xFF111827)
val CharcoalCard = Color(0xFF1E293B)
val CharcoalCardHover = Color(0xFF334155)
val GlassBorder = Color(0x3394A3B8)
val GlassBorderSubtle = Color(0x1A94A3B8)

val ElectricBlue = Color(0xFF3B82F6)
val ElectricBlueLight = Color(0xFF60A5FA)
val NeonCyan = Color(0xFF06B6D4)
val VividViolet = Color(0xFF8B5CF6)
val MintEmerald = Color(0xFF10B981)
val CoralRed = Color(0xFFEF4444)
val CrimsonCoral = Color(0xFFEF4444)
val WarmAmber = Color(0xFFF59E0B)
val CyberPink = Color(0xFFEC4899)
val SoftPink = Color(0xFFEC4899)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val LightBg = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE5EAF2)
val TextPrimaryLight = Color(0xFF172033)
val TextSecondaryLight = Color(0xFF64748B)
val TextMutedLight = Color(0xFF94A3B8)
