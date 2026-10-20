package app.theone.android.diary.journal.frontend

import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryEntryLabelsTest {
    @Test
    fun `moodLabel resolves a known emoji`() {
        assertEquals("Strong", moodLabel("💪"))
    }

    @Test
    fun `moodLabel is blank for an unknown emoji`() {
        assertEquals("", moodLabel("🤔"))
    }
}
