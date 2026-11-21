package app.theone.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Home, the profile drawer, Account details and Settings are neutral only - black, grey and white
// (design handoff: home). Fixed, so neither dynamic colour nor a section accent tints them.
private val NeutralColorScheme = lightColorScheme(
    primary = WarmOnSurface,
    onPrimary = Color.White,
    secondaryContainer = NeutralSelected,
    onSecondaryContainer = WarmOnSurface,
    background = WarmSurface,
    onBackground = WarmOnSurface,
    surface = WarmSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = NeutralIconTile,
    onSurfaceVariant = WarmIcon,
    outline = WarmButtonOutline,
    outlineVariant = WarmCardBorder,
    error = DeleteRed,
    onError = Color.White
)

@Composable
fun NeutralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeutralColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
