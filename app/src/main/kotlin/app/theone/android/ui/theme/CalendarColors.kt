package app.theone.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Calendar tokens (calendar redesign handoff 2a, light and dark). Values are final, not derived. */
data class CalendarPalette(
    val surface: Color,
    val card: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val bodySecondary: Color,
    val muted: Color,
    val hint: Color,
    val iconTint: Color,
    val outlineSegmented: Color,
    val outlineButton: Color,
    val cardBorder: Color,
    val divider: Color,
    val innerDivider: Color,
    val emptyCell: Color,
    val statTile: Color,
    val chipBorder: Color,
    val dashedBorder: Color,
    val chevronStrong: Color,
    val chevronWeak: Color,
    val hover: Color,
    val progressTrack: Color,
    // Section accent (SectionAccent.Calendar) and its derived tones.
    val accentContainer: Color,
    val accentOnContainer: Color,
    val accentSoft: Color,
    val accentSolid: Color,
    val onAccentSolid: Color,
    // Activity colours.
    val workoutDot: Color,
    val workoutTint: Color,
    val workoutSoft: Color,
    val workoutInk: Color,
    val diaryDot: Color,
    val diaryTint: Color,
    val diarySoft: Color,
    val diaryInk: Color,
    val mealDot: Color,
    val mealTint: Color,
    val mealInk: Color,
    // 5a (training planner handoff): the Meals stat tile.
    val mealSoft: Color,
    val mealNumber: Color
)

val LightCalendarPalette = CalendarPalette(
    surface = Color(0xFFFFFBFE),
    card = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B20),
    secondaryText = Color(0xFF6B655F),
    bodySecondary = Color(0xFF3B3733),
    muted = Color(0xFFA39C95),
    hint = Color(0xFF8A837C),
    iconTint = Color(0xFF49454F),
    outlineSegmented = Color(0xFF79747E),
    outlineButton = Color(0xFFCFC6BE),
    cardBorder = Color(0xFFEAE4DE),
    divider = Color(0xFFEEE8E2),
    innerDivider = Color(0xFFF0EBE6),
    emptyCell = Color(0xFFF4F1EE),
    statTile = Color(0xFFF7F3EF),
    chipBorder = Color(0xFFE2DBD4),
    dashedBorder = Color(0xFFDDD5CE),
    chevronStrong = Color(0xFF9A938C),
    chevronWeak = Color(0xFFB5AEA7),
    hover = Color(0xFFF3EDE7),
    progressTrack = Color(0xFFE6DFD8),
    accentContainer = Color(0xFFFEE8CC),
    accentOnContainer = Color(0xFF713F00),
    accentSoft = Color(0xFFFFF3E3),
    accentSolid = Color(0xFFFB8C00),
    onAccentSolid = Color(0xFF1F1300),
    workoutDot = Color(0xFF1E88E5),
    workoutTint = Color(0xFFD2E7FA),
    workoutSoft = Color(0xFFEEF5FD),
    workoutInk = Color(0xFF0D5BA6),
    diaryDot = Color(0xFF8E24AA),
    diaryTint = Color(0xFFE8D3EE),
    diarySoft = Color(0xFFF6EEF8),
    diaryInk = Color(0xFF6A1B86),
    mealDot = Color(0xFF43A047),
    mealTint = Color(0xFFD9ECDA),
    mealInk = Color(0xFF2E7D32),
    mealSoft = Color(0xFFEEF6EE),
    mealNumber = Color(0xFF1B5E20)
)

// From the design file's own light -> dark conversion (2a dark). Two deviations, per the dark-mode
// handoff README: hint uses #8B8783 (the converted #6F6B67 is below 4.5:1), and text on the solid
// accent is #1E1E1E (rule 2) rather than the converted near-white, which is unreadable on #F1994C.
// #69: the workout / meal / diary tints are deeper than the handoff (#19222A / #1A221A / #3D3041), which
// were too close to tell apart in the month cells (blue vs green dE 11.6, now 37; all pairs >= 20).
val DarkCalendarPalette = CalendarPalette(
    surface = Color(0xFF121012),
    card = Color(0xFF1E1E1E),
    onSurface = Color(0xFFDBD9DE),
    secondaryText = Color(0xFF8B8783),
    bodySecondary = Color(0xFFBBB8B5),
    muted = Color(0xFF595551),
    hint = Color(0xFF8B8783),
    iconTint = Color(0xFFA9A6AD),
    outlineSegmented = Color(0xFF7A777D),
    outlineButton = Color(0xFF383430),
    cardBorder = Color(0xFF221F1D),
    divider = Color(0xFF1F1D1A),
    innerDivider = Color(0xFF1D1B19),
    emptyCell = Color(0xFF191716),
    statTile = Color(0xFF181614),
    chipBorder = Color(0xFF282522),
    dashedBorder = Color(0xFF2D2926),
    chevronStrong = Color(0xFF615D59),
    chevronWeak = Color(0xFF4A4643),
    hover = Color(0xFF1C1917),
    progressTrack = Color(0xFF26221F),
    accentContainer = Color(0xFF312412),
    accentOnContainer = Color(0xFFFFC99A),
    accentSoft = Color(0xFF1A150E),
    accentSolid = Color(0xFFF1994C),
    onAccentSolid = Color(0xFF1E1E1E),
    workoutDot = Color(0xFF8DC4FF),
    workoutTint = Color(0xFF1C3350),
    workoutSoft = Color(0xFF14161A),
    workoutInk = Color(0xFFB1D3FB),
    diaryDot = Color(0xFFECB6FE),
    diaryTint = Color(0xFF4A2A4C),
    diarySoft = Color(0xFF1A171B),
    diaryInk = Color(0xFFECC7FD),
    mealDot = Color(0xFF82D583),
    mealTint = Color(0xFF1F3F27),
    mealInk = Color(0xFF91E091),
    mealSoft = Color(0xFF141814),
    mealNumber = Color(0xFFA5E9A4)
)

/** `CalendarColors.Surface` etc., resolving light or dark from the current theme. */
object CalendarColors {
    private val p: CalendarPalette
        @Composable @ReadOnlyComposable
        get() = if (LocalOneColors.current.isDark) DarkCalendarPalette else LightCalendarPalette

    val Surface: Color
        @Composable @ReadOnlyComposable
        get() = p.surface

    val Card: Color
        @Composable @ReadOnlyComposable
        get() = p.card

    val OnSurface: Color
        @Composable @ReadOnlyComposable
        get() = p.onSurface

    val SecondaryText: Color
        @Composable @ReadOnlyComposable
        get() = p.secondaryText

    val BodySecondary: Color
        @Composable @ReadOnlyComposable
        get() = p.bodySecondary

    val Muted: Color
        @Composable @ReadOnlyComposable
        get() = p.muted

    val Hint: Color
        @Composable @ReadOnlyComposable
        get() = p.hint

    val IconTint: Color
        @Composable @ReadOnlyComposable
        get() = p.iconTint

    val OutlineSegmented: Color
        @Composable @ReadOnlyComposable
        get() = p.outlineSegmented

    val OutlineButton: Color
        @Composable @ReadOnlyComposable
        get() = p.outlineButton

    val CardBorder: Color
        @Composable @ReadOnlyComposable
        get() = p.cardBorder

    val Divider: Color
        @Composable @ReadOnlyComposable
        get() = p.divider

    val InnerDivider: Color
        @Composable @ReadOnlyComposable
        get() = p.innerDivider

    val EmptyCell: Color
        @Composable @ReadOnlyComposable
        get() = p.emptyCell

    val StatTile: Color
        @Composable @ReadOnlyComposable
        get() = p.statTile

    val ChipBorder: Color
        @Composable @ReadOnlyComposable
        get() = p.chipBorder

    val DashedBorder: Color
        @Composable @ReadOnlyComposable
        get() = p.dashedBorder

    val ChevronStrong: Color
        @Composable @ReadOnlyComposable
        get() = p.chevronStrong

    val ChevronWeak: Color
        @Composable @ReadOnlyComposable
        get() = p.chevronWeak

    val Hover: Color
        @Composable @ReadOnlyComposable
        get() = p.hover

    val ProgressTrack: Color
        @Composable @ReadOnlyComposable
        get() = p.progressTrack

    val AccentContainer: Color
        @Composable @ReadOnlyComposable
        get() = p.accentContainer

    val AccentOnContainer: Color
        @Composable @ReadOnlyComposable
        get() = p.accentOnContainer

    val AccentSoft: Color
        @Composable @ReadOnlyComposable
        get() = p.accentSoft

    val AccentSolid: Color
        @Composable @ReadOnlyComposable
        get() = p.accentSolid

    val OnAccentSolid: Color
        @Composable @ReadOnlyComposable
        get() = p.onAccentSolid

    val WorkoutDot: Color
        @Composable @ReadOnlyComposable
        get() = p.workoutDot

    val WorkoutTint: Color
        @Composable @ReadOnlyComposable
        get() = p.workoutTint

    val WorkoutSoft: Color
        @Composable @ReadOnlyComposable
        get() = p.workoutSoft

    val WorkoutInk: Color
        @Composable @ReadOnlyComposable
        get() = p.workoutInk

    val DiaryDot: Color
        @Composable @ReadOnlyComposable
        get() = p.diaryDot

    val DiaryTint: Color
        @Composable @ReadOnlyComposable
        get() = p.diaryTint

    val DiarySoft: Color
        @Composable @ReadOnlyComposable
        get() = p.diarySoft

    val DiaryInk: Color
        @Composable @ReadOnlyComposable
        get() = p.diaryInk

    val MealDot: Color
        @Composable @ReadOnlyComposable
        get() = p.mealDot

    val MealTint: Color
        @Composable @ReadOnlyComposable
        get() = p.mealTint

    val MealInk: Color
        @Composable @ReadOnlyComposable
        get() = p.mealInk

    val MealSoft: Color
        @Composable @ReadOnlyComposable
        get() = p.mealSoft

    val MealNumber: Color
        @Composable @ReadOnlyComposable
        get() = p.mealNumber
}
