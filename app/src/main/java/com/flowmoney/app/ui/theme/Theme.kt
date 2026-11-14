package com.flowmoney.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FlowPrimary,
    onPrimary = FlowTextPrimary,
    primaryContainer = FlowDarkSurfaceVariant,
    onPrimaryContainer = FlowPrimaryLight,
    secondary = FlowSecondary,
    onSecondary = FlowDarkBackground,
    background = FlowDarkBackground,
    onBackground = FlowTextPrimary,
    surface = FlowDarkSurface,
    onSurface = FlowTextPrimary,
    surfaceVariant = FlowDarkSurfaceVariant,
    onSurfaceVariant = FlowTextSecondary,
    outline = FlowDarkBorder,
    error = FlowExpense,
    onError = FlowTextPrimary
)

@Composable
fun FlowMoneyTheme(
    darkTheme: Boolean = true, // Default Theme is Dark Mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = FlowDarkBackground.toArgb()
                window.navigationBarColor = FlowDarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FlowTypography,
        content = content
    )
}
