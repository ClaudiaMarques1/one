package app.theone.android.settings.backend

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
}
