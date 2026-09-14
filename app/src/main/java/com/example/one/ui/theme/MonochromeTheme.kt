package com.example.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MonochromeColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF9E9E9E),
    error = Color(0xFFB00020),
    onError = Color.White
)

/** Fixed black-and-white theme, independent of system light/dark mode or per-section accents. */
@Composable
fun MonochromeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
