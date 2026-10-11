package app.theone.android.diary

import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryEntryTest {
    @Test
    fun `effectiveSoreness uses soreness when present`() {
        val entry = DiaryEntry(sorePartIds = listOf("quads"), soreness = mapOf("quads" to 3, "back" to 1))
        assertEquals(mapOf("quads" to 3, "back" to 1), entry.effectiveSoreness)
    }

    @Test
    fun `effectiveSoreness falls back to sorePartIds at level 2 for legacy entries`() {
        val entry = DiaryEntry(sorePartIds = listOf("quads", "shoulders"), soreness = emptyMap())
        assertEquals(mapOf("quads" to 2, "shoulders" to 2), entry.effectiveSoreness)
    }

    @Test
    fun `effectiveSoreness is empty when neither field is set`() {
        assertEquals(emptyMap<String, Int>(), DiaryEntry().effectiveSoreness)
    }

    @Test
    fun `moodLabel resolves a known emoji`() {
        assertEquals("Strong", moodLabel("💪"))
    }

    @Test
    fun `moodLabel is blank for an unknown emoji`() {
        assertEquals("", moodLabel("🤔"))
    }
}
