package com.studysphere.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Light Color Scheme ───────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary            = Indigo600,
    onPrimary          = Color.White,
    primaryContainer   = Indigo100,
    onPrimaryContainer = Indigo900,

    secondary          = Violet500,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF3B0764),

    tertiary           = Teal500,
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),

    error              = Red500,
    onError            = Color.White,
    errorContainer     = Red100,
    onErrorContainer   = Red600,

    background         = Slate50,
    onBackground       = Slate900,

    surface            = Color.White,
    onSurface          = Slate900,
    surfaceVariant     = Slate100,
    onSurfaceVariant   = Slate600,

    outline            = Slate200,
    outlineVariant     = Slate100,

    inverseSurface     = Slate800,
    inverseOnSurface   = Slate50,
    inversePrimary     = Indigo400,

    scrim              = Color.Black.copy(alpha = 0.4f),
    surfaceTint        = Indigo600
)

// ─── Dark Color Scheme ────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = Indigo400,
    onPrimary          = Indigo900,
    primaryContainer   = Indigo700,
    onPrimaryContainer = Indigo200,

    secondary          = Color(0xFFC4B5FD),
    onSecondary        = Color(0xFF2E1065),
    secondaryContainer = Violet600,
    onSecondaryContainer = Color(0xFFEDE9FE),

    tertiary           = Color(0xFF5EEAD4),
    onTertiary         = Color(0xFF134E4A),
    tertiaryContainer  = Color(0xFF0F766E),
    onTertiaryContainer = Color(0xFFCCFBF1),

    error              = Red400,
    onError            = Color(0xFF7F1D1D),
    errorContainer     = Color(0xFF991B1B),
    onErrorContainer   = Red100,

    background         = DarkBg,
    onBackground       = Color(0xFFE8EDF5),

    surface            = DarkSurface,
    onSurface          = Color(0xFFE8EDF5),
    surfaceVariant     = DarkCard,
    onSurfaceVariant   = DarkMuted,

    outline            = DarkBorder,
    outlineVariant     = Color(0xFF1E2845),

    inverseSurface     = Slate100,
    inverseOnSurface   = Slate900,
    inversePrimary     = Indigo600,

    scrim              = Color.Black.copy(alpha = 0.6f),
    surfaceTint        = Indigo400
)

// ─── Theme Composition Local ──────────────────────────────────────────────────

val LocalDarkTheme = compositionLocalOf { false }

// ─── StudySphere Theme ────────────────────────────────────────────────────────

@Composable
fun StudySphereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content
        )
    }
}
