package app.theone.android.workout.templates.frontend

import app.theone.android.workout.templates.backend.TemplateSet
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateLabelsTest {
    @Test
    fun `setSummary with no sets`() {
        assertEquals("No sets yet", setSummary(emptyList()))
    }

    @Test
    fun `setSummary collapses identical sets`() {
        assertEquals("3 × 10 · 35 kg", setSummary(List(3) { TemplateSet(10, 35.0) }))
    }

    @Test
    fun `setSummary lists reps and the weight range when sets differ`() {
        val sets = listOf(TemplateSet(8, 60.0), TemplateSet(8, 60.0), TemplateSet(6, 62.5), TemplateSet(6, 62.5))
        assertEquals("4 sets · 8, 8, 6, 6 reps · 60–62.5 kg", setSummary(sets))
    }

    @Test
    fun `setSummary shows one weight when only reps differ`() {
        assertEquals("2 sets · 10, 8 reps · 40 kg", setSummary(listOf(TemplateSet(10, 40.0), TemplateSet(8, 40.0))))
    }

    @Test
    fun `setSummary says bodyweight for weight 0`() {
        assertEquals("3 × 12 · bodyweight", setSummary(List(3) { TemplateSet(12, 0.0) }))
        assertEquals("2 sets · 12, 10 reps · bodyweight", setSummary(listOf(TemplateSet(12, 0.0), TemplateSet(10, 0.0))))
    }
}
