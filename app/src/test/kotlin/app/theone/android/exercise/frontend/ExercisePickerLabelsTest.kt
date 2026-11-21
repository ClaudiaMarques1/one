package app.theone.android.exercise.frontend

import app.theone.android.exercise.backend.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerLabelsTest {
    @Test
    fun `toDisplayLabel title-cases each word`() {
        assertEquals("Upper Arms", "upper arms".toDisplayLabel())
        assertEquals("Chest", "chest".toDisplayLabel())
    }

    @Test
    fun `metaLabel joins target and equipment`() {
        val exercise = Exercise("1", "Bench Press", "chest", "barbell", "pectorals", emptyList(), emptyList(), "", "")
        assertEquals("Pectorals · Barbell", exercise.metaLabel())
    }

    @Test
    fun `confirmLabel by selection count`() {
        assertEquals("Select exercises", confirmLabel(0))
        assertEquals("Add 1 exercise", confirmLabel(1))
        assertEquals("Add 3 exercises", confirmLabel(3))
    }
}
