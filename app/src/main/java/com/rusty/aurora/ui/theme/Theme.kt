package com.rusty.aurora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Always dark, matching the Echo Show dashboard - this app deliberately
 * doesn't follow the system light/dark setting or Material You dynamic
 * color, since the whole point is looking like one product with the
 * dashboard rather than blending into whatever theme the rest of the
 * phone happens to be in.
 *
 * [weatherCondition] drives the accent color exactly like the dashboard's
 * applyAccentColor() - pass the current WeatherSnapshot.condition (or null
 * before the first successful fetch) and the whole screen's accent
 * (buttons, highlighted values, icons) updates together.
 */
@Composable
fun AuroraTheme(
    weatherCondition: String? = null,
    content: @Composable () -> Unit
) {
    val accent = accentColorForCondition(weatherCondition)

    val colorScheme = remember(accent) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            secondary = accent,
            background = AuroraBackground,
            onBackground = AuroraTextPrimary,
            surface = AuroraSurface,
            onSurface = AuroraTextPrimary,
            surfaceVariant = AuroraSurfaceElevated,
            onSurfaceVariant = AuroraTextSecondary,
            error = AuroraDanger,
            errorContainer = AuroraDanger.copy(alpha = 0.18f),
            onErrorContainer = AuroraTextPrimary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
