package app.theone.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light and dark tokens (design handoffs: dark mode, home). Screens keep reading the named tokens
// below (`WarmSurface`, `WorkoutInk` ...); each is a composable getter on [LocalOneColors], which
// OneTheme sets from the Theme setting and NeutralTheme overrides with the Home palette.

/** Every themed colour token. [isDark] lets SectionTheme pick its dark branch. */
data class OneColors(
    val isDark: Boolean,
    // Warm neutrals shared by the section screens.
    val warmSurface: Color,
    val card: Color,
    val warmOnSurface: Color,
    val warmBodyText: Color,
    val warmSecondaryText: Color,
    val warmHint: Color,
    val warmIcon: Color,
    val warmMutedIcon: Color,
    val warmHandle: Color,
    val warmButtonOutline: Color,
    val warmCardBorder: Color,
    val warmDivider: Color,
    val warmField: Color,
    val warmDisabledFill: Color,
    val warmPressedRow: Color,
    // Home and its sub-screens, plus the bottom nav bar - neutral only.
    val neutralIconTile: Color,
    val neutralAvatar: Color,
    val neutralAvatarInitials: Color,
    val neutralNavBar: Color,
    val neutralSelected: Color,
    val neutralSwitchOff: Color,
    val neutralSwitchOnThumb: Color,
    val signOutPressed: Color,
    val drawer: Color,
    // Workout section (the accent fill is WorkoutAccent).
    val workoutInk: Color,
    val workoutDeep: Color,
    val onWorkoutDeep: Color,
    val workoutContainer: Color,
    val workoutSoft: Color,
    val workoutSelectedRow: Color,
    val workoutRestProgress: Color,
    val workoutRestSecondary: Color,
    val workoutDashedOutline: Color,
    // Session detail (3a, #48): comparison "up" text and the PR badge.
    val progressUp: Color,
    val prBadge: Color,
    val onPrBadge: Color,
    // Diary section and goals.
    val diaryInk: Color,
    val diaryDeepInk: Color,
    val diaryMenuSurface: Color,
    val diarySelected: Color,
    // Part-of-day labels on habit reminder times.
    val morningLabelBackground: Color,
    val morningLabelText: Color,
    val afternoonLabelBackground: Color,
    val afternoonLabelText: Color,
    val eveningLabelBackground: Color,
    val eveningLabelText: Color,
    val nightLabelBackground: Color,
    val nightLabelText: Color,
    // Shared.
    val deleteRed: Color,
    val scrim: Color
)

val LightOneColors = OneColors(
    isDark = false,
    warmSurface = Color(0xFFFFFBFE),
    card = Color(0xFFFFFFFF),
    warmOnSurface = Color(0xFF1D1B20),
    warmBodyText = Color(0xFF3B3733),
    warmSecondaryText = Color(0xFF6B655F),
    warmHint = Color(0xFF8A837C),
    warmIcon = Color(0xFF49454F),
    warmMutedIcon = Color(0xFF9A938C),
    warmHandle = Color(0xFFB5AEA7),
    warmButtonOutline = Color(0xFFCFC6BE),
    warmCardBorder = Color(0xFFEAE4DE),
    warmDivider = Color(0xFFF0EBE6),
    warmField = Color(0xFFF4F1EE),
    warmDisabledFill = Color(0xFFE6E0E9),
    warmPressedRow = Color(0xFFF3EDE7),
    neutralIconTile = Color(0xFFEFEDEA),
    neutralAvatar = Color(0xFF2B2926),
    neutralAvatarInitials = Color(0xFFFFFFFF),
    neutralNavBar = Color(0xFFF2F0ED),
    neutralSelected = Color(0xFFE2DFDB),
    neutralSwitchOff = Color(0xFF79747E),
    neutralSwitchOnThumb = Color(0xFFFFFFFF),
    signOutPressed = Color(0xFFFCEEEE),
    drawer = Color(0xFFFFFBFE),
    workoutInk = Color(0xFF0D5BA6),
    workoutDeep = Color(0xFF0A3F75),
    onWorkoutDeep = Color(0xFFFFFFFF),
    workoutContainer = Color(0xFFD2E7FA),
    workoutSoft = Color(0xFFEEF5FD),
    workoutSelectedRow = Color(0xFFF5F9FE),
    workoutRestProgress = Color(0xFF64B5F6),
    workoutRestSecondary = Color(0xFFBBDDF9),
    workoutDashedOutline = Color(0xFF9EC3E8),
    progressUp = Color(0xFF2E7D32),
    prBadge = Color(0xFFFFE3B3),
    onPrBadge = Color(0xFF7A4A00),
    diaryInk = Color(0xFF6A1B86),
    diaryDeepInk = Color(0xFF4A1260),
    diaryMenuSurface = Color(0xFFF7F2FA),
    diarySelected = Color(0xFFEBDDF0),
    morningLabelBackground = Color(0xFFFFF1D6),
    morningLabelText = Color(0xFF7A4A00),
    afternoonLabelBackground = Color(0xFFE3F0FB),
    afternoonLabelText = Color(0xFF0D5BA6),
    eveningLabelBackground = Color(0xFFF6E3EC),
    eveningLabelText = Color(0xFF8A1F4F),
    nightLabelBackground = Color(0xFFE6E3F5),
    nightLabelText = Color(0xFF3A2F80),
    deleteRed = Color(0xFFB3261E),
    scrim = Color(0xFF1D1B20).copy(alpha = 0.32f)
)

// Dark-mode handoff token tables. WarmHint uses the secondary-text value: its converted #6F6B67 is
// below 4.5:1 on the dark surface (handoff "Notes to check during build").
val DarkOneColors = OneColors(
    isDark = true,
    warmSurface = Color(0xFF121012),
    card = Color(0xFF1E1E1E),
    warmOnSurface = Color(0xFFDBD9DE),
    warmBodyText = Color(0xFFBBB8B5),
    warmSecondaryText = Color(0xFF8B8783),
    warmHint = Color(0xFF8B8783),
    warmIcon = Color(0xFFA9A6AD),
    warmMutedIcon = Color(0xFF615D59),
    warmHandle = Color(0xFF4A4643),
    warmButtonOutline = Color(0xFF383430),
    warmCardBorder = Color(0xFF221F1D),
    warmDivider = Color(0xFF1D1B19),
    warmField = Color(0xFF191716),
    warmDisabledFill = Color(0xFF232024),
    warmPressedRow = Color(0xFF2A2725),
    neutralIconTile = Color(0xFF2A2725),
    neutralAvatar = Color(0xFFD9D5D0),
    neutralAvatarInitials = Color(0xFF141312),
    neutralNavBar = Color(0xFF1C1B19),
    neutralSelected = Color(0xFF3A3734),
    neutralSwitchOff = Color(0xFF938F8A),
    neutralSwitchOnThumb = Color(0xFF141312),
    signOutPressed = Color(0xFF3A1F1E),
    drawer = Color(0xFF121012),
    workoutInk = Color(0xFFB1D3FB),
    workoutDeep = Color(0xFFC4DEFD),
    onWorkoutDeep = Color(0xFF1E1E1E),
    workoutContainer = Color(0xFF19222A),
    workoutSoft = Color(0xFF14161A),
    workoutSelectedRow = Color(0xFF121415),
    workoutRestProgress = Color(0xFF67B8F9),
    workoutRestSecondary = Color(0xFF243B4D),
    workoutDashedOutline = Color(0xFF8BAFD3),
    progressUp = Color(0xFF91E091),
    prBadge = Color(0xFF392704),
    onPrBadge = Color(0xFFFDC589),
    diaryInk = Color(0xFFECC7FD),
    diaryDeepInk = Color(0xFFF1D5FF),
    diaryMenuSurface = Color(0xFF171518),
    diarySelected = Color(0xFF251F27),
    morningLabelBackground = Color(0xFF271F0F),
    morningLabelText = Color(0xFFFDC589),
    afternoonLabelBackground = Color(0xFF151B1F),
    afternoonLabelText = Color(0xFFB1D3FB),
    eveningLabelBackground = Color(0xFF231B1F),
    eveningLabelText = Color(0xFFFEC0D3),
    nightLabelBackground = Color(0xFF201E26),
    nightLabelText = Color(0xFFD6D7FD),
    deleteRed = Color(0xFFFEB8AD),
    scrim = Color.Black.copy(alpha = 0.6f)
)

// Home, the drawer, Account details and Settings (home handoff): light uses the shared tokens with a
// plain black scrim; dark is hand-tuned rather than converted.
val HomeLightOneColors = LightOneColors.copy(scrim = Color.Black.copy(alpha = 0.32f))

val HomeDarkOneColors = DarkOneColors.copy(
    warmSurface = Color(0xFF141312),
    card = Color(0xFF1F1D1B),
    drawer = Color(0xFF1F1D1B),
    warmCardBorder = Color(0xFF2E2B28),
    warmDivider = Color(0xFF2A2725),
    warmOnSurface = Color(0xFFE8E4E0),
    warmSecondaryText = Color(0xFFA39D97),
    warmIcon = Color(0xFFCAC4BE),
    warmMutedIcon = Color(0xFF7D7771),
    warmButtonOutline = Color(0xFF4A4642),
    warmDisabledFill = Color(0xFF2A2725),
    deleteRed = Color(0xFFF2B8B5)
)

val LocalOneColors = staticCompositionLocalOf { LightOneColors }

/** The four section accents; [dark] is the dark-mode accent from the handoff table. */
enum class SectionAccent(val light: Color, val dark: Color) {
    Calendar(Color(0xFFFB8C00), Color(0xFFF1994C)),
    Workout(Color(0xFF1E88E5), Color(0xFF8DC4FF)),
    Meals(Color(0xFF2E7D32), Color(0xFF91E091)),
    Diary(Color(0xFF8E24AA), Color(0xFFECB6FE));

    val current: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalOneColors.current.isDark) dark else light
}

val CalendarAccent: Color
    @Composable @ReadOnlyComposable
    get() = SectionAccent.Calendar.current
val WorkoutAccent: Color
    @Composable @ReadOnlyComposable
    get() = SectionAccent.Workout.current
val MealsAccent: Color
    @Composable @ReadOnlyComposable
    get() = SectionAccent.Meals.current
val DiaryAccent: Color
    @Composable @ReadOnlyComposable
    get() = SectionAccent.Diary.current

private val colors: OneColors
    @Composable @ReadOnlyComposable
    get() = LocalOneColors.current

val ProgressUp: Color
    @Composable @ReadOnlyComposable
    get() = colors.progressUp
val PrBadge: Color
    @Composable @ReadOnlyComposable
    get() = colors.prBadge
val OnPrBadge: Color
    @Composable @ReadOnlyComposable
    get() = colors.onPrBadge

val WarmSurface: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmSurface
val CardSurface: Color
    @Composable @ReadOnlyComposable
    get() = colors.card
val WarmOnSurface: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmOnSurface
val WarmBodyText: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmBodyText
val WarmSecondaryText: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmSecondaryText
val WarmHint: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmHint
val WarmIcon: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmIcon
val WarmMutedIcon: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmMutedIcon
val WarmHandle: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmHandle
val WarmButtonOutline: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmButtonOutline
val WarmCardBorder: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmCardBorder
val WarmDivider: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmDivider
val WarmField: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmField
val WarmDisabledFill: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmDisabledFill
val WarmPressedRow: Color
    @Composable @ReadOnlyComposable
    get() = colors.warmPressedRow

val NeutralIconTile: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralIconTile
val NeutralAvatar: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralAvatar
val NeutralAvatarInitials: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralAvatarInitials
val NeutralNavBar: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralNavBar
val NeutralSelected: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralSelected
val NeutralSwitchOff: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralSwitchOff
val NeutralSwitchOnThumb: Color
    @Composable @ReadOnlyComposable
    get() = colors.neutralSwitchOnThumb
val SignOutPressed: Color
    @Composable @ReadOnlyComposable
    get() = colors.signOutPressed

val WorkoutInk: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutInk
val WorkoutDeep: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutDeep
val WorkoutContainer: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutContainer
val WorkoutSoft: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutSoft
val WorkoutSelectedRow: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutSelectedRow
val WorkoutRestProgress: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutRestProgress
val WorkoutRestSecondary: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutRestSecondary
val WorkoutDashedOutline: Color
    @Composable @ReadOnlyComposable
    get() = colors.workoutDashedOutline

val DiaryInk: Color
    @Composable @ReadOnlyComposable
    get() = colors.diaryInk
val DiaryDeepInk: Color
    @Composable @ReadOnlyComposable
    get() = colors.diaryDeepInk
val DiaryMenuSurface: Color
    @Composable @ReadOnlyComposable
    get() = colors.diaryMenuSurface
val DiarySelected: Color
    @Composable @ReadOnlyComposable
    get() = colors.diarySelected

val MorningLabelBackground: Color
    @Composable @ReadOnlyComposable
    get() = colors.morningLabelBackground
val MorningLabelText: Color
    @Composable @ReadOnlyComposable
    get() = colors.morningLabelText
val AfternoonLabelBackground: Color
    @Composable @ReadOnlyComposable
    get() = colors.afternoonLabelBackground
val AfternoonLabelText: Color
    @Composable @ReadOnlyComposable
    get() = colors.afternoonLabelText
val EveningLabelBackground: Color
    @Composable @ReadOnlyComposable
    get() = colors.eveningLabelBackground
val EveningLabelText: Color
    @Composable @ReadOnlyComposable
    get() = colors.eveningLabelText
val NightLabelBackground: Color
    @Composable @ReadOnlyComposable
    get() = colors.nightLabelBackground
val NightLabelText: Color
    @Composable @ReadOnlyComposable
    get() = colors.nightLabelText

val DeleteRed: Color
    @Composable @ReadOnlyComposable
    get() = colors.deleteRed
val ScrimColor: Color
    @Composable @ReadOnlyComposable
    get() = colors.scrim

val DrawerSurface: Color
    @Composable @ReadOnlyComposable
    get() = colors.drawer

/** Text, icons and the Skip button fill on the rest bar (WorkoutDeep): white in light, #1E1E1E in dark. */
val OnWorkoutDeep: Color
    @Composable @ReadOnlyComposable
    get() = colors.onWorkoutDeep
