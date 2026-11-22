package app.theone.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Diary journal tokens not shared with other sections (diary journal handoff 1a / 2a, light and dark). */
data class DiaryPalette(
    val container: Color,
    val soft: Color,
    val todayHelper: Color,
    val energyPartial: Color,
    val energyPartialText: Color,
    val outlineSegmented: Color,
    val stepperFill: Color,
    val soreMild: Color,
    val soreSore: Color,
    val soreVery: Color,
    val figureNeutral: Color,
    val figureIdle: Color
)

val LightDiaryPalette = DiaryPalette(
    container = Color(0xFFF3E3F7),
    soft = Color(0xFFF6EEF8),
    todayHelper = Color(0xFF5E5560),
    energyPartial = Color(0xFFC99AD6),
    energyPartialText = Color(0xFF3D0F50),
    outlineSegmented = Color(0xFF79747E),
    stepperFill = Color(0xFFFAF7F4),
    soreMild = Color(0xFFD9B3E4),
    soreSore = Color(0xFFAD5BC4),
    soreVery = Color(0xFF6A1B86),
    figureNeutral = Color(0xFFECE6E0),
    figureIdle = Color(0xFFD9D0C8)
)

// From the design file's own light -> dark conversion (1a / 2a dark, "Diary purple" levels - the
// handoff default). One deviation: text on the partial energy segments is #1E1E1E (dark-mode README
// rule 2) - the converted #F1DCFD on #CB9CD8 is about 1.6:1.
val DarkDiaryPalette = DiaryPalette(
    container = Color(0xFF211A23),
    soft = Color(0xFF1A171B),
    todayHelper = Color(0xFF99939A),
    energyPartial = Color(0xFFCB9CD8),
    energyPartialText = Color(0xFF1E1E1E),
    outlineSegmented = Color(0xFF7A777D),
    stepperFill = Color(0xFF151413),
    soreMild = Color(0xFFC09BCB),
    soreSore = Color(0xFFE6A2FA),
    soreVery = Color(0xFFECC7FD),
    figureNeutral = Color(0xFF211E1B),
    figureIdle = Color(0xFF312C29)
)

/** `DiaryColors.Container` etc., resolving light or dark from the current theme. */
object DiaryColors {
    private val p: DiaryPalette
        @Composable @ReadOnlyComposable
        get() = if (LocalOneColors.current.isDark) DarkDiaryPalette else LightDiaryPalette

    val Container: Color
        @Composable @ReadOnlyComposable
        get() = p.container

    val Soft: Color
        @Composable @ReadOnlyComposable
        get() = p.soft

    val TodayHelper: Color
        @Composable @ReadOnlyComposable
        get() = p.todayHelper

    val EnergyPartial: Color
        @Composable @ReadOnlyComposable
        get() = p.energyPartial

    val EnergyPartialText: Color
        @Composable @ReadOnlyComposable
        get() = p.energyPartialText

    val OutlineSegmented: Color
        @Composable @ReadOnlyComposable
        get() = p.outlineSegmented

    val StepperFill: Color
        @Composable @ReadOnlyComposable
        get() = p.stepperFill

    val SoreMild: Color
        @Composable @ReadOnlyComposable
        get() = p.soreMild

    val SoreSore: Color
        @Composable @ReadOnlyComposable
        get() = p.soreSore

    val SoreVery: Color
        @Composable @ReadOnlyComposable
        get() = p.soreVery

    val FigureNeutral: Color
        @Composable @ReadOnlyComposable
        get() = p.figureNeutral

    val FigureIdle: Color
        @Composable @ReadOnlyComposable
        get() = p.figureIdle
}
