package com.naman.focusos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// THEME ACCESSORS (FOCUS OS & RUPEEFLOW UNIFIED)
// =============================================================================

object FocusTheme {
    val colors: FocusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusColors.current.isDark
}

object AppTheme {
    val colors: FocusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusColors.current.isDark
}

// =============================================================================
// MATERIAL 3 COLOR SCHEMES FOR ALL 4 THEMES
// =============================================================================

private val PeachyColorScheme = lightColorScheme(
    primary = PeachyFocusColors.primaryAccent,
    onPrimary = PeachyFocusColors.onPrimaryAccent,
    primaryContainer = PeachyFocusColors.primaryAccentContainer,
    onPrimaryContainer = PeachyFocusColors.textPrimary,
    secondary = PeachyFocusColors.deepPeach,
    onSecondary = Color.White,
    secondaryContainer = PeachyFocusColors.surfaceSecondary,
    onSecondaryContainer = PeachyFocusColors.textPrimary,
    tertiary = PeachyFocusColors.mint,
    onTertiary = PeachyFocusColors.textPrimary,
    tertiaryContainer = PeachyFocusColors.surfaceSecondary,
    onTertiaryContainer = PeachyFocusColors.success,
    background = PeachyFocusColors.background,
    onBackground = PeachyFocusColors.textPrimary,
    surface = PeachyFocusColors.surface,
    onSurface = PeachyFocusColors.textPrimary,
    surfaceVariant = PeachyFocusColors.surfaceSecondary,
    onSurfaceVariant = PeachyFocusColors.textSecondary,
    outline = PeachyFocusColors.cardBorder,
    outlineVariant = PeachyFocusColors.divider,
    error = PeachyFocusColors.error,
    onError = PeachyFocusColors.onError,
    errorContainer = PeachyFocusColors.errorContainer,
    onErrorContainer = PeachyFocusColors.error
)

private val LavenderColorScheme = lightColorScheme(
    primary = LavenderFocusColors.primaryAccent,
    onPrimary = LavenderFocusColors.onPrimaryAccent,
    primaryContainer = LavenderFocusColors.primaryAccentContainer,
    onPrimaryContainer = LavenderFocusColors.textPrimary,
    secondary = LavenderFocusColors.info,
    onSecondary = Color.White,
    secondaryContainer = LavenderFocusColors.surfaceSecondary,
    onSecondaryContainer = LavenderFocusColors.textPrimary,
    tertiary = LavenderFocusColors.accentPink,
    onTertiary = LavenderFocusColors.textPrimary,
    tertiaryContainer = LavenderFocusColors.surfaceSecondary,
    onTertiaryContainer = LavenderFocusColors.textPrimary,
    background = LavenderFocusColors.background,
    onBackground = LavenderFocusColors.textPrimary,
    surface = LavenderFocusColors.surface,
    onSurface = LavenderFocusColors.textPrimary,
    surfaceVariant = LavenderFocusColors.surfaceSecondary,
    onSurfaceVariant = LavenderFocusColors.textSecondary,
    outline = LavenderFocusColors.cardBorder,
    outlineVariant = LavenderFocusColors.divider,
    error = LavenderFocusColors.error,
    onError = LavenderFocusColors.onError,
    errorContainer = LavenderFocusColors.errorContainer,
    onErrorContainer = LavenderFocusColors.error
)

private val MidnightMochiColorScheme = darkColorScheme(
    primary = MidnightMochiFocusColors.primaryAccent,
    onPrimary = MidnightMochiFocusColors.onPrimaryAccent,
    primaryContainer = MidnightMochiFocusColors.primaryAccentContainer,
    onPrimaryContainer = MidnightMochiFocusColors.textPrimary,
    secondary = MidnightMochiFocusColors.info,
    onSecondary = MidnightMochiFocusColors.background,
    secondaryContainer = MidnightMochiFocusColors.surfaceSecondary,
    onSecondaryContainer = MidnightMochiFocusColors.info,
    tertiary = MidnightMochiFocusColors.accentPink,
    onTertiary = MidnightMochiFocusColors.background,
    tertiaryContainer = MidnightMochiFocusColors.surfaceSecondary,
    onTertiaryContainer = MidnightMochiFocusColors.textPrimary,
    background = MidnightMochiFocusColors.background,
    onBackground = MidnightMochiFocusColors.textPrimary,
    surface = MidnightMochiFocusColors.surface,
    onSurface = MidnightMochiFocusColors.textPrimary,
    surfaceVariant = MidnightMochiFocusColors.surfaceSecondary,
    onSurfaceVariant = MidnightMochiFocusColors.textSecondary,
    outline = MidnightMochiFocusColors.cardBorder,
    outlineVariant = MidnightMochiFocusColors.divider,
    error = MidnightMochiFocusColors.error,
    onError = MidnightMochiFocusColors.textPrimary,
    errorContainer = MidnightMochiFocusColors.errorContainer,
    onErrorContainer = MidnightMochiFocusColors.error
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFocusColors.primaryAccent,
    onPrimary = DarkFocusColors.onPrimaryAccent,
    primaryContainer = DarkFocusColors.primaryAccentContainer,
    onPrimaryContainer = DarkFocusColors.textPrimary,
    secondary = DarkFocusColors.info,
    onSecondary = DarkFocusColors.background,
    secondaryContainer = DarkFocusColors.surfaceSecondary,
    onSecondaryContainer = DarkFocusColors.info,
    tertiary = DarkFocusColors.success,
    onTertiary = DarkFocusColors.background,
    tertiaryContainer = DarkFocusColors.surfaceSecondary,
    onTertiaryContainer = DarkFocusColors.success,
    background = DarkFocusColors.background,
    onBackground = DarkFocusColors.textPrimary,
    surface = DarkFocusColors.surface,
    onSurface = DarkFocusColors.textPrimary,
    surfaceVariant = DarkFocusColors.surfaceSecondary,
    onSurfaceVariant = DarkFocusColors.textSecondary,
    outline = DarkFocusColors.cardBorder,
    outlineVariant = DarkFocusColors.divider,
    error = DarkFocusColors.error,
    onError = DarkFocusColors.textPrimary,
    errorContainer = DarkFocusColors.errorContainer,
    onErrorContainer = DarkFocusColors.error
)

private val LightColorScheme = lightColorScheme(
    primary = LightFocusColors.primaryAccent,
    onPrimary = LightFocusColors.onPrimaryAccent,
    primaryContainer = LightFocusColors.primaryAccentContainer,
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = LightFocusColors.info,
    onSecondary = LightFocusColors.onPrimaryAccent,
    secondaryContainer = LightFocusColors.infoContainer,
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = LightFocusColors.success,
    onTertiary = LightFocusColors.onPrimaryAccent,
    tertiaryContainer = LightFocusColors.successContainer,
    onTertiaryContainer = Color(0xFF065F46),
    background = LightFocusColors.background,
    onBackground = LightFocusColors.textPrimary,
    surface = LightFocusColors.surface,
    onSurface = LightFocusColors.textPrimary,
    surfaceVariant = LightFocusColors.surfaceSecondary,
    onSurfaceVariant = LightFocusColors.textSecondary,
    outline = LightFocusColors.cardBorder,
    outlineVariant = LightFocusColors.divider,
    error = LightFocusColors.error,
    onError = LightFocusColors.onError,
    errorContainer = LightFocusColors.errorContainer,
    onErrorContainer = Color(0xFF991B1B)
)

// =============================================================================
// MAIN THEME COMPOSABLE
// =============================================================================

@Composable
fun FocusOSTheme(
    themeMode: String = "PEACHY", // "PEACHY", "LAVENDER", "MIDNIGHT_MOCHI", "DARK", "LIGHT", "SYSTEM"
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val (focusColors, colorScheme) = when (themeMode.uppercase()) {
        "PEACHY" -> Pair(PeachyFocusColors, PeachyColorScheme)
        "LAVENDER" -> Pair(LavenderFocusColors, LavenderColorScheme)
        "MIDNIGHT_MOCHI" -> Pair(MidnightMochiFocusColors, MidnightMochiColorScheme)
        "DARK" -> Pair(DarkFocusColors, DarkColorScheme)
        "LIGHT" -> Pair(LightFocusColors, LightColorScheme)
        "SYSTEM" -> if (isSystemDark) Pair(DarkFocusColors, DarkColorScheme) else Pair(PeachyFocusColors, PeachyColorScheme)
        else -> if (darkTheme) Pair(DarkFocusColors, DarkColorScheme) else Pair(PeachyFocusColors, PeachyColorScheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = focusColors.background.toArgb()
                window.navigationBarColor = focusColors.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !focusColors.isDark
                insetsController.isAppearanceLightNavigationBars = !focusColors.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalFocusColors provides focusColors,
        LocalAppColors provides focusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FocusTypography,
            content = content
        )
    }
}

@Composable
fun FocusOSTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    FocusOSTheme(
        themeMode = if (darkTheme) "DARK" else "PEACHY",
        darkTheme = darkTheme,
        content = content
    )
}

@Composable
fun RupeeFlowTheme(
    themeMode: String = "PEACHY",
    content: @Composable () -> Unit
) {
    FocusOSTheme(
        themeMode = themeMode,
        content = content
    )
}
