package com.kindlevibe.reader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = InkBlue,
    onPrimary        = CreamWhite,
    primaryContainer = Color(0xFFDDE7FF),
    secondary        = GoldAccent,
    onSecondary      = InkBlueDark,
    secondaryContainer = Color(0xFFFFF0C8),
    background       = SurfaceLight,
    onBackground     = TextPrimary,
    surface          = CardLight,
    onSurface        = TextPrimary,
    surfaceVariant   = SoftGray,
    onSurfaceVariant = TextSecondary,
    outline          = Color(0xFFCBD0D8)
)

private val DarkColorScheme = darkColorScheme(
    primary          = GoldLight,
    onPrimary        = InkBlueDark,
    primaryContainer = Color(0xFF1E3A5F),
    secondary        = GoldAccent,
    onSecondary      = InkBlueDark,
    secondaryContainer = Color(0xFF2D2010),
    background       = SurfaceDark,
    onBackground     = TextOnDark,
    surface          = CardDark,
    onSurface        = TextOnDark,
    surfaceVariant   = Color(0xFF252D38),
    onSurfaceVariant = TextMuted,
    outline          = Color(0xFF3A4452)
)

@Composable
fun KindleVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
