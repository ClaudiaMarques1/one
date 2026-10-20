package app.theone.android.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` (which `viewModelScope` uses) with a test dispatcher, so work a
 * ViewModel starts in `viewModelScope.launch` runs on the JVM. Use as
 * `@get:Rule val mainDispatcherRule = MainDispatcherRule()`.
 *
 * The default [UnconfinedTestDispatcher] runs launched coroutines eagerly, so a ViewModel call has
 * finished by the time the next test line runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
