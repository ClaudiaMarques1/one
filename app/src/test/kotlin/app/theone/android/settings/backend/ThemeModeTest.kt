package app.theone.android.settings.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `system follows the phone`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun `light and dark ignore the phone`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun `stored names round-trip and unknown values fall back to system`() {
        ThemeMode.entries.forEach { assertEquals(it, themeModeFromStored(it.name)) }
        assertEquals(ThemeMode.SYSTEM, themeModeFromStored(null))
        assertEquals(ThemeMode.SYSTEM, themeModeFromStored("SEPIA"))
    }
}
