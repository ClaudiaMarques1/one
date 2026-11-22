package app.theone.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * The app theme. [darkTheme] comes from Settings -> Appearance -> Theme (MainActivity resolves it).
 * No dynamic colour: the wallpaper palette would replace the section accents (dark-mode handoff).
 */
@Composable
fun OneTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkOneColors else LightOneColors
    OneColorsTheme(colors, primary = colors.warmOnSurface, onPrimary = colors.onInk(), content = content)
}

/** Provides [colors] to the named tokens and a matching Material [ColorScheme]. */
@Composable
internal fun OneColorsTheme(colors: OneColors, primary: Color, onPrimary: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOneColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(primary, onPrimary),
            typography = Typography,
            content = content
        )
    }
}

/** Text on a strong fill: white in light, the dark surface in dark (Material 3 dark). */
internal fun OneColors.onInk(): Color = if (isDark) warmSurface else Color.White

private fun OneColors.toColorScheme(primary: Color, onPrimary: Color): ColorScheme {
    val base = if (isDark) {
        darkColorScheme(
            // Menus, dialogs and sheets sit on these; keep them a step above the surface, like cards.
            surfaceContainerLowest = warmSurface,
            surfaceContainerLow = warmField,
            surfaceContainer = card,
            surfaceContainerHigh = card,
            surfaceContainerHighest = warmDisabledFill
        )
    } else {
        lightColorScheme()
    }
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondaryContainer = neutralSelected,
        onSecondaryContainer = warmOnSurface,
        background = warmSurface,
        onBackground = warmOnSurface,
        surface = warmSurface,
        onSurface = warmOnSurface,
        surfaceVariant = neutralIconTile,
        onSurfaceVariant = warmIcon,
        outline = warmButtonOutline,
        outlineVariant = warmCardBorder,
        error = deleteRed,
        onError = onInk(),
        scrim = Color.Black
    )
}
