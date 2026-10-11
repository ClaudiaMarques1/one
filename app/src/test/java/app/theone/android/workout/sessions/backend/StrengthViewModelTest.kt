package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers the active-session state management only, which is pure SavedStateHandle mutation with
// no repository call - StrengthRepository is a final, Firebase-coupled class with no seam for a
// fake, so saveSession() itself isn't covered here.
class StrengthViewModelTest {
    private val viewModel = StrengthViewModel(
        repository = StrengthRepository(),
        savedStateHandle = SavedStateHandle()
    )

    @Test
    fun `startSessionFromTemplate populates active session state from the template`() {
        val template = WorkoutTemplate(
            id = "t1",
            name = "Push Day",
            exercises = listOf(TemplateExercise(name = "Bench Press", sets = listOf(TemplateSet(reps = 8, weightKg = 60.0))))
        )

        viewModel.startSessionFromTemplate(template)

        assertEquals("Push Day", viewModel.activeTemplateName.value)
        assertEquals(1, viewModel.currentExercises.value.size)
        assertEquals("Bench Press", viewModel.currentExercises.value[0].name)
        assertEquals(listOf(StrengthSet(reps = 8, weightKg = 60.0)), viewModel.currentExercises.value[0].sets)
        assertTrue(viewModel.sessionStartTimeMillis.value != null)
    }

    @Test
    fun `discardSession clears active session state`() {
        viewModel.startSessionFromTemplate(WorkoutTemplate(id = "t1", name = "Push Day"))

        viewModel.discardSession()

        assertEquals("", viewModel.activeTemplateName.value)
        assertTrue(viewModel.currentExercises.value.isEmpty())
        assertNull(viewModel.sessionStartTimeMillis.value)
    }

    @Test
    fun `setExercises preserves logged sets for names that already exist`() {
        viewModel.startSessionFromTemplate(
            WorkoutTemplate(
                id = "t1",
                name = "Push Day",
                exercises = listOf(TemplateExercise(name = "Bench Press", sets = listOf(TemplateSet(reps = 8, weightKg = 60.0))))
            )
        )

        viewModel.setExercises(listOf("Bench Press", "Lateral Raise"))

        val exercises = viewModel.currentExercises.value
        assertEquals(listOf(StrengthSet(reps = 8, weightKg = 60.0)), exercises.first { it.name == "Bench Press" }.sets)
        assertTrue(exercises.first { it.name == "Lateral Raise" }.sets.isEmpty())
    }

    @Test
    fun `addSet appends a set to the given exercise`() {
        viewModel.setExercises(listOf("Bench Press"))

        viewModel.addSet(0, StrengthSet(reps = 10, weightKg = 40.0))

        assertEquals(listOf(StrengthSet(reps = 10, weightKg = 40.0)), viewModel.currentExercises.value[0].sets)
    }

    @Test
    fun `updateSet replaces the set at the given index`() {
        viewModel.setExercises(listOf("Bench Press"))
        viewModel.addSet(0, StrengthSet(reps = 10, weightKg = 40.0))

        viewModel.updateSet(0, 0, StrengthSet(reps = 12, weightKg = 42.5))

        assertEquals(StrengthSet(reps = 12, weightKg = 42.5), viewModel.currentExercises.value[0].sets[0])
    }

    @Test
    fun `removeSet removes the set at the given index`() {
        viewModel.setExercises(listOf("Bench Press"))
        viewModel.addSet(0, StrengthSet(reps = 10, weightKg = 40.0))
        viewModel.addSet(0, StrengthSet(reps = 8, weightKg = 45.0))

        viewModel.removeSet(0, 0)

        assertEquals(listOf(StrengthSet(reps = 8, weightKg = 45.0)), viewModel.currentExercises.value[0].sets)
    }

    @Test
    fun `removeExercise removes at index`() {
        viewModel.setExercises(listOf("Bench Press", "Squat"))

        viewModel.removeExercise(0)

        assertEquals(listOf("Squat"), viewModel.currentExercises.value.map { it.name })
    }

    @Test
    fun `duplicateExercise inserts a copy right after the original`() {
        viewModel.setExercises(listOf("Bench Press", "Squat"))

        viewModel.duplicateExercise(0)

        val names = viewModel.currentExercises.value.map { it.name }
        assertEquals(listOf("Bench Press", "Bench Press", "Squat"), names)
    }
}
