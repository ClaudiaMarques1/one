package com.example.one.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Wraps [content] in a MaterialTheme whose primary/secondary roles are derived from [accent],
 * so each app section (Workout, Meals, Diary, Calendar) can carry its own color identity
 * without redefining a full tonal palette.
 */
@Composable
fun SectionTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.withAccent(accent),
        typography = MaterialTheme.typography,
        content = content
    )
}

private fun ColorScheme.withAccent(accent: Color): ColorScheme {
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val container = lerp(accent, Color.White, 0.8f)
    val onContainer = lerp(accent, Color.Black, 0.55f)
    return copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = container,
        onSecondaryContainer = onContainer
    )
}
