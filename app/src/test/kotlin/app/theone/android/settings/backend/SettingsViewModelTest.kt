package app.theone.android.settings.backend

import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `vibrateOnRestEnd defaults to on`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.vibrateOnRestEnd.collect {} }

        assertTrue(viewModel.vibrateOnRestEnd.value)
    }

    @Test
    fun `vibrateOnRestEnd reflects the stored value`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(vibrateOnRestEnd = false))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.vibrateOnRestEnd.collect {} }

        assertFalse(viewModel.vibrateOnRestEnd.value)
    }

    @Test
    fun `setVibrateOnRestEnd stores the value and updates the flow`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.vibrateOnRestEnd.collect {} }

        viewModel.setVibrateOnRestEnd(false)

        assertFalse(repository.storedVibrateOnRestEnd)
        assertFalse(viewModel.vibrateOnRestEnd.value)
        assertNull(viewModel.settingsError.value)
    }

    @Test
    fun `dndDuringWorkout defaults to off and can be turned on`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.dndDuringWorkout.collect {} }
        assertFalse(viewModel.dndDuringWorkout.value)

        viewModel.setDndDuringWorkout(true)

        assertTrue(repository.storedDndDuringWorkout)
        assertTrue(viewModel.dndDuringWorkout.value)
    }

    @Test
    fun `a failed write surfaces settingsError and keeps the old value`() = runTest {
        val repository = FakeSettingsRepository().apply { failNext = IllegalStateException("disk full") }
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.vibrateOnRestEnd.collect {} }

        viewModel.setVibrateOnRestEnd(false)

        assertEquals("disk full", viewModel.settingsError.value)
        assertTrue(viewModel.vibrateOnRestEnd.value)

        viewModel.clearSettingsError()
        assertNull(viewModel.settingsError.value)
    }

    @Test
    fun `mealSettings default to the handoff values and store changes`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.mealSettings.collect {} }

        assertEquals(MealSettings(), viewModel.mealSettings.value)
        assertEquals(2500, viewModel.mealSettings.value.trainingTargets.kcal)

        val changed = MealSettings(enabledSlots = MealSlot.entries.toSet(), waterGlasses = 12)
        viewModel.setMealSettings(changed)

        assertEquals(changed, repository.storedMealSettings)
        assertEquals(changed, viewModel.mealSettings.value)
    }

    @Test
    fun `themeMode defaults to system`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.themeMode.collect {} }

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode stores the value and updates the flow`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.themeMode.collect {} }

        viewModel.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.storedThemeMode)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertNull(viewModel.settingsError.value)
    }

    @Test
    fun `a failed theme write surfaces settingsError and keeps the old value`() = runTest {
        val repository = FakeSettingsRepository(themeMode = ThemeMode.LIGHT)
            .apply { failNext = IllegalStateException("disk full") }
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.themeMode.collect {} }

        viewModel.setThemeMode(ThemeMode.DARK)

        assertEquals("disk full", viewModel.settingsError.value)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun `setWeekStart stores the app-wide week start and updates the flow`() = runTest {
        val repository = FakeSettingsRepository(weekStart = WeekStart.MONDAY)
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.weekStart.collect {} }

        assertEquals(WeekStart.MONDAY, viewModel.weekStart.value)

        viewModel.setWeekStart(WeekStart.SUNDAY)

        assertEquals(WeekStart.SUNDAY, repository.storedWeekStart)
        assertEquals(WeekStart.SUNDAY, viewModel.weekStart.value)
    }

    @Test
    fun `WeekStart maps to java Calendar day numbers`() {
        assertEquals(java.util.Calendar.MONDAY, WeekStart.MONDAY.toCalendarDay())
        assertEquals(java.util.Calendar.SUNDAY, WeekStart.SUNDAY.toCalendarDay())
        assertEquals(WeekStart.SUNDAY, WeekStart.fromLocale(java.util.Locale.US))
        assertEquals(WeekStart.MONDAY, WeekStart.fromLocale(java.util.Locale.UK))
    }
}
