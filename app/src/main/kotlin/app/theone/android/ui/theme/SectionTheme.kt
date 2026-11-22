package app.theone.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Wraps [content] in a MaterialTheme whose primary/secondary roles are derived from [accent],
 * so each app section (Workout, Meals, Diary, Calendar) can carry its own color identity
 * without redefining a full tonal palette. In dark mode it uses the accent's dark variant.
 */
@Composable
fun SectionTheme(accent: SectionAccent, content: @Composable () -> Unit) {
    val colors = LocalOneColors.current
    MaterialTheme(
        colorScheme = if (colors.isDark) {
            MaterialTheme.colorScheme.withDarkAccent(accent.dark, colors.warmSurface)
        } else {
            MaterialTheme.colorScheme.withAccent(accent.light)
        },
        typography = MaterialTheme.typography,
        content = content
    )
}

private fun ColorScheme.withAccent(accent: Color): ColorScheme {
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    return withAccentRoles(
        accent = accent,
        onAccent = onAccent,
        container = lerp(accent, Color.White, 0.8f),
        onContainer = lerp(accent, Color.Black, 0.55f)
    )
}

// Dark branch from the dark-mode handoff: on-accent text flips to #1E1E1E, containers are dark tints.
private fun ColorScheme.withDarkAccent(accent: Color, darkSurface: Color): ColorScheme = withAccentRoles(
    accent = accent,
    onAccent = Color(0xFF1E1E1E),
    container = lerp(accent, darkSurface, 0.8f),
    onContainer = lerp(accent, Color.White, 0.6f)
)

private fun ColorScheme.withAccentRoles(accent: Color, onAccent: Color, container: Color, onContainer: Color) = copy(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = container,
    onPrimaryContainer = onContainer,
    secondary = accent,
    onSecondary = onAccent,
    secondaryContainer = container,
    onSecondaryContainer = onContainer
)
