package app.theone.android.workout.templates.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers the draft-management logic only, which is pure local state with no repository call -
// WorkoutTemplateRepository is a final, Firebase-coupled class with no seam for a fake, so
// saveDraftTemplate()/deleteTemplate() aren't covered here.
class WorkoutTemplateViewModelTest {

    private val viewModel = WorkoutTemplateViewModel(repository = WorkoutTemplateRepository())

    @Test
    fun `updateDraftName and updateDraftDescription update their flows`() {
        viewModel.updateDraftName("Push Day")
        viewModel.updateDraftDescription("Chest, shoulders, triceps")

        assertEquals("Push Day", viewModel.draftName.value)
        assertEquals("Chest, shoulders, triceps", viewModel.draftDescription.value)
    }

    @Test
    fun `setDraftExercises preserves sets for names that already exist`() {
        // Seed a draft exercise that already has sets (as startEditingTemplate would from Firestore).
        viewModel.startEditingTemplate(
            WorkoutTemplate(
                id = "t1",
                name = "Push Day",
                exercises = listOf(TemplateExercise(name = "Bench Press", sets = listOf(TemplateSet(reps = 8, weightKg = 60.0))))
            )
        )

        // Re-picking the same name plus a new one must keep Bench Press's existing sets,
        // and only the new name should come back blank.
        viewModel.setDraftExercises(listOf("Bench Press", "Squat"))

        val exercises = viewModel.draftExercises.value
        assertEquals(listOf(TemplateSet(reps = 8, weightKg = 60.0)), exercises.first { it.name == "Bench Press" }.sets)
        assertTrue(exercises.first { it.name == "Squat" }.sets.isEmpty())
    }

    @Test
    fun `setDraftExercises drops names no longer in the list`() {
        viewModel.setDraftExercises(listOf("Bench Press", "Squat"))
        viewModel.setDraftExercises(listOf("Squat"))

        val names = viewModel.draftExercises.value.map { it.name }
        assertEquals(listOf("Squat"), names)
    }

    @Test
    fun `removeDraftExercise removes at index`() {
        viewModel.setDraftExercises(listOf("Bench Press", "Squat"))
        viewModel.removeDraftExercise(0)

        assertEquals(listOf("Squat"), viewModel.draftExercises.value.map { it.name })
    }

    @Test
    fun `startEditingTemplate populates editingTemplate name description and exercises`() {
        val template = WorkoutTemplate(
            id = "t1",
            name = "Push Day",
            description = "Chest day",
            exercises = listOf(TemplateExercise("Bench Press"))
        )

        viewModel.startEditingTemplate(template)

        assertEquals(template, viewModel.editingTemplate.value)
        assertEquals("Push Day", viewModel.draftName.value)
        assertEquals("Chest day", viewModel.draftDescription.value)
        assertEquals(template.exercises, viewModel.draftExercises.value)
    }

    @Test
    fun `clearDraft resets name description exercises and editingTemplate`() {
        viewModel.startEditingTemplate(
            WorkoutTemplate(id = "t1", name = "Push Day", description = "Chest day", exercises = listOf(TemplateExercise("Bench Press")))
        )

        viewModel.clearDraft()

        assertEquals("", viewModel.draftName.value)
        assertEquals("", viewModel.draftDescription.value)
        assertTrue(viewModel.draftExercises.value.isEmpty())
        assertNull(viewModel.editingTemplate.value)
    }
}
