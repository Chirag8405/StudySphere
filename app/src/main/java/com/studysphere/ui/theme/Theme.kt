package com.studysphere.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Light Color Scheme (Pure White) ──────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary            = PrimaryPurple,
    onPrimary          = PureWhite,
    primaryContainer   = Gray100,
    onPrimaryContainer = Gray900,

    secondary          = Gray800,
    onSecondary        = PureWhite,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,

    tertiary           = Gray600,
    onTertiary         = PureWhite,
    tertiaryContainer  = Gray50,
    onTertiaryContainer = Gray900,

    error              = Red600,
    onError            = PureWhite,
    errorContainer     = Color(0xFFFEE2E2),
    onErrorContainer   = Red600,

    background         = PureWhite,
    onBackground       = PureBlack,

    surface            = PureWhite,
    onSurface          = PureBlack,
    surfaceVariant     = Gray50,
    onSurfaceVariant   = Gray500,

    outline            = Gray200,
    outlineVariant     = Gray100,

    inverseSurface     = Gray900,
    inverseOnSurface   = Gray50,
    inversePrimary     = PrimaryPurple,

    scrim              = PureBlack.copy(alpha = 0.4f),
    surfaceTint        = Color.Transparent,
    surfaceDim         = Gray100
)

// ─── Dark Color Scheme (Pure Black) ───────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryPurple,
    onPrimary          = PureWhite,
    primaryContainer   = Gray800,
    onPrimaryContainer = PureWhite,

    secondary          = Gray200,
    onSecondary        = PureBlack,
    secondaryContainer = Gray800,
    onSecondaryContainer = PureWhite,

    tertiary           = Gray400,
    onTertiary         = PureBlack,
    tertiaryContainer  = PureBlack,
    onTertiaryContainer = PureWhite,

    error              = Red500,
    onError            = PureWhite,
    errorContainer     = Color(0xFF450A0A),
    onErrorContainer   = Red500,

    background         = PureBlack,
    onBackground       = PureWhite,

    surface            = PureBlack, // Pitch black
    onSurface          = PureWhite,
    surfaceVariant     = DarkCard,
    onSurfaceVariant   = DarkMuted,

    outline            = DarkBorder,
    outlineVariant     = Gray800,

    inverseSurface     = PureWhite,
    inverseOnSurface   = PureBlack,
    inversePrimary     = PrimaryPurple,

    scrim              = PureBlack.copy(alpha = 0.6f),
    surfaceTint        = Color.Transparent,
    surfaceDim         = DarkSurface
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
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = StudySphereTypography,
            content     = content
        )
    }
}
