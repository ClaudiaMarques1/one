package app.theone.android.settings.frontend

import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsSectionsTest {
    @Test
    fun `sections follow the app's sections and round-trip through their route keys`() {
        assertEquals(listOf("Calendar", "Workout", "Meals", "Diary"), SettingsSection.entries.map { it.title })
        SettingsSection.entries.forEach { assertEquals(it, SettingsSection.fromKey(it.key)) }
        assertNull(SettingsSection.fromKey("appearance")) // Appearance lives on the Settings screen itself
        assertNull(SettingsSection.fromKey(null))
    }

    @Test
    fun `summaries describe each section's current values`() {
        assertEquals("Vibrate when rest ends: On", workoutSummary(true))
        assertEquals("Vibrate when rest ends: Off", workoutSummary(false))
        assertEquals("4 slots · 2.5 L water", mealsSummary(MealSettings()))
        assertEquals("1 slot · 3 L water", mealsSummary(MealSettings(enabledSlots = setOf(MealSlot.LUNCH), waterGlasses = 12)))
        assertEquals("No settings yet", NO_SETTINGS_YET)
    }
}
