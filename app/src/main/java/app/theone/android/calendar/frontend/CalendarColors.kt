package app.theone.android.calendar.frontend

import androidx.compose.ui.graphics.Color

/** Design tokens from the calendar redesign handoff (option 2a). Values are final, not derived. */
internal object CalendarColors {
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1D1B20)
    val SecondaryText = Color(0xFF6B655F)
    val BodySecondary = Color(0xFF3B3733)
    val Muted = Color(0xFFA39C95)
    val Hint = Color(0xFF8A837C)
    val IconTint = Color(0xFF49454F)
    val OutlineSegmented = Color(0xFF79747E)
    val OutlineButton = Color(0xFFCFC6BE)
    val CardBorder = Color(0xFFEAE4DE)
    val Divider = Color(0xFFEEE8E2)
    val InnerDivider = Color(0xFFF0EBE6)
    val EmptyCell = Color(0xFFF4F1EE)
    val StatTile = Color(0xFFF7F3EF)
    val ChipBorder = Color(0xFFE2DBD4)
    val DashedBorder = Color(0xFFDDD5CE)
    val ChevronStrong = Color(0xFF9A938C)
    val ChevronWeak = Color(0xFFB5AEA7)
    val Hover = Color(0xFFF3EDE7)

    // Section accent (Destination.Calendar = #FB8C00) and its derived tones.
    val AccentContainer = Color(0xFFFEE8CC)
    val AccentOnContainer = Color(0xFF713F00)
    val AccentSoft = Color(0xFFFFF3E3)
    val AccentSolid = Color(0xFFFB8C00)
    val OnAccentSolid = Color(0xFF1F1300)

    // Activity colours.
    val WorkoutDot = Color(0xFF1E88E5)
    val WorkoutTint = Color(0xFFD2E7FA)
    val WorkoutSoft = Color(0xFFEEF5FD)
    val WorkoutInk = Color(0xFF0D5BA6)

    val DiaryDot = Color(0xFF8E24AA)
    val DiaryTint = Color(0xFFE8D3EE)
    val DiarySoft = Color(0xFFF6EEF8)
    val DiaryInk = Color(0xFF6A1B86)

    val MealDot = Color(0xFF43A047)
    val MealTint = Color(0xFFD9ECDA)
    val MealInk = Color(0xFF2E7D32)
}
