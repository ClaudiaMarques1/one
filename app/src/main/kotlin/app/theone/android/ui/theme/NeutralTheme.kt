package app.theone.android.ui.theme

import androidx.compose.runtime.Composable

// Home, the profile drawer, Account details and Settings are neutral only - black, grey and white
// (design handoff: home). Fixed, so neither dynamic colour nor a section accent tints them; dark uses
// the hand-tuned Home tokens rather than the converted section ones.
@Composable
fun NeutralTheme(content: @Composable () -> Unit) {
    val colors = if (LocalOneColors.current.isDark) HomeDarkOneColors else HomeLightOneColors
    OneColorsTheme(colors, primary = colors.warmOnSurface, onPrimary = colors.onInk(), content = content)
}
