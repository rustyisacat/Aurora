package com.rusty.aurora.ui.theme

import androidx.compose.ui.graphics.Color

// Matches echo-dashboard's style.css palette (:root custom properties) so
// the phone app and the Echo Show dashboard read as one product, not two.
// True black background, dark-elevated surfaces - Material dark-theme
// style, cards read as distinct layers above the background.
val AuroraBackground = Color(0xFF000000)
val AuroraSurface = Color(0xFF121212)
val AuroraSurfaceElevated = Color(0xFF1C1C1E)

val AuroraTextPrimary = Color(0xFFF5F5F5)
val AuroraTextSecondary = Color(0xFF9A9A9A)
val AuroraTextTertiary = Color(0xFF6B6B6B)

// Dynamic accent, same three tones and the same weather->tone mapping as
// script.js's ACCENT_BY_CONDITION - warm for clear skies, gray for cloud,
// blue for anything wet. AuroraTheme picks one of these based on the
// current WeatherSnapshot; AccentGray is the default shown before the
// first successful weather fetch.
val AuroraAccentWarm = Color(0xFFFFB74D)
val AuroraAccentGray = Color(0xFF9AA5B1)
val AuroraAccentBlue = Color(0xFF5B9BF0)

val AuroraDanger = Color(0xFFFF6B6B)

/** Mirrors script.js's ACCENT_BY_CONDITION - keep the two in sync. */
fun accentColorForCondition(condition: String?): Color = when (condition) {
    "Clear" -> AuroraAccentWarm
    "Drizzle", "Rain", "Rain Showers", "Snow", "Snow Showers", "Thunderstorm" -> AuroraAccentBlue
    else -> AuroraAccentGray
}
