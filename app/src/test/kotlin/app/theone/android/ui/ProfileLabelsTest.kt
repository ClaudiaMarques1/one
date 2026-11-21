package app.theone.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileLabelsTest {
    @Test
    fun `initials use the first and last words of the name`() {
        assertEquals("AL", profileInitials("Ada Lovelace", "ada@example.com"))
        assertEquals("AK", profileInitials("  ada  byron  king ", ""))
    }

    @Test
    fun `a single-word name gives one letter`() {
        assertEquals("A", profileInitials("ada", "x@example.com"))
    }

    @Test
    fun `no name falls back to the email, then to a question mark`() {
        assertEquals("C", profileInitials("", "claudia@example.com"))
        assertEquals("?", profileInitials(" ", ""))
    }
}
