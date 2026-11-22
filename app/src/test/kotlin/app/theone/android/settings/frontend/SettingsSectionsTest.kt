package app.theone.android.settings.frontend

import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.WeekStart
import app.theone.android.settings.backend.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsSectionsTest {
    @Test
    fun `sections round-trip through their route keys`() {
        SettingsSection.entries.forEach { assertEquals(it, SettingsSection.fromKey(it.key)) }
        assertNull(SettingsSection.fromKey("nope"))
        assertNull(SettingsSection.fromKey(null))
    }

    @Test
    fun `summaries describe each section's current values`() {
        assertEquals("Theme: System", appearanceSummary(ThemeMode.SYSTEM))
        assertEquals("Theme: Dark", appearanceSummary(ThemeMode.DARK))
        assertEquals("Vibrate when rest ends: On", workoutSummary(true))
        assertEquals("Vibrate when rest ends: Off", workoutSummary(false))
        assertEquals("Week starts Monday · 4 slots", mealsSummary(MealSettings(weekStart = WeekStart.MONDAY)))
        assertEquals(
            "Week starts Sunday · 1 slot",
            mealsSummary(MealSettings(weekStart = WeekStart.SUNDAY, enabledSlots = setOf(MealSlot.LUNCH)))
        )
    }
}
