package com.skinbud.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RoseQuartz,
    onPrimary = PureWhite,
    primaryContainer = LightRose,
    onPrimaryContainer = DeepRose,
    secondary = SageEmerald,
    onSecondary = PureWhite,
    secondaryContainer = LightSage,
    onSecondaryContainer = DeepSage,
    tertiary = WarmChampagne,
    onTertiary = PureWhite,
    tertiaryContainer = LightChampagne,
    background = SilkPorcelain,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = MutedCard,
    onSurfaceVariant = TextSecondaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = RoseQuartz,
    onPrimary = PureWhite,
    primaryContainer = DeepRose,
    onPrimaryContainer = LightRose,
    secondary = SageEmerald,
    onSecondary = PureWhite,
    secondaryContainer = DeepSage,
    onSecondaryContainer = LightSage,
    tertiary = WarmChampagne,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun SkinBudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
