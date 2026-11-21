package app.theone.android.workout.templates.backend

import app.theone.android.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutTemplateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWorkoutTemplateRepository()

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { WorkoutTemplateViewModel(repository) }

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
    fun `moveDraftExercise moves an exercise and keeps its sets`() {
        viewModel.startEditingTemplate(
            WorkoutTemplate(
                id = "t1",
                name = "Push Day",
                exercises = listOf(
                    TemplateExercise(name = "Bench Press", sets = listOf(TemplateSet(reps = 8, weightKg = 60.0))),
                    TemplateExercise(name = "Squat"),
                    TemplateExercise(name = "Row")
                )
            )
        )

        viewModel.moveDraftExercise(from = 0, to = 2)

        val exercises = viewModel.draftExercises.value
        assertEquals(listOf("Squat", "Row", "Bench Press"), exercises.map { it.name })
        assertEquals(listOf(TemplateSet(reps = 8, weightKg = 60.0)), exercises.last().sets)

        viewModel.moveDraftExercise(from = 2, to = 0)

        assertEquals(listOf("Bench Press", "Squat", "Row"), viewModel.draftExercises.value.map { it.name })
    }

    @Test
    fun `moveDraftExercise ignores out-of-range and same-position moves`() {
        viewModel.setDraftExercises(listOf("Bench Press", "Squat"))

        viewModel.moveDraftExercise(from = 0, to = 5)
        viewModel.moveDraftExercise(from = -1, to = 0)
        viewModel.moveDraftExercise(from = 1, to = 1)

        assertEquals(listOf("Bench Press", "Squat"), viewModel.draftExercises.value.map { it.name })
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

    @Test
    fun `templates exposes the repository's templates newest first`() = runTest {
        repository.updateTemplate(WorkoutTemplate(id = "old", createdAt = 1L))
        repository.updateTemplate(WorkoutTemplate(id = "new", createdAt = 2L))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.templates.collect {} }

        assertEquals(listOf("new", "old"), viewModel.templates.value.map { it.id })
    }

    @Test
    fun `saveDraftTemplate adds a new template and clears the draft`() {
        viewModel.updateDraftName("Push Day")
        viewModel.updateDraftDescription("Chest day")
        viewModel.setDraftExercises(listOf("Bench Press"))

        viewModel.saveDraftTemplate()

        val saved = repository.stored.single()
        assertEquals("Push Day", saved.name)
        assertEquals("Chest day", saved.description)
        assertEquals(listOf("Bench Press"), saved.exercises.map { it.name })
        assertEquals("", viewModel.draftName.value)
        assertTrue(viewModel.draftExercises.value.isEmpty())
    }

    @Test
    fun `saveDraftTemplate does nothing without a name or exercises`() {
        viewModel.updateDraftName("No exercises")
        viewModel.saveDraftTemplate()

        assertTrue(repository.stored.isEmpty())
        assertEquals("No exercises", viewModel.draftName.value)
    }

    @Test
    fun `saveDraftTemplate while editing updates the template in place`() = runTest {
        val original = WorkoutTemplate(id = "t1", name = "Push Day", exercises = listOf(TemplateExercise("Bench Press")), createdAt = 5L)
        repository.updateTemplate(original)
        viewModel.startEditingTemplate(original)
        viewModel.updateDraftName("Push Day A")

        viewModel.saveDraftTemplate()

        val updated = repository.stored.single()
        assertEquals("t1", updated.id)
        assertEquals("Push Day A", updated.name)
        assertEquals(5L, updated.createdAt)
        assertNull(viewModel.editingTemplate.value)
    }

    @Test
    fun `deleteTemplate removes the template`() = runTest {
        repository.updateTemplate(WorkoutTemplate(id = "t1"))
        repository.updateTemplate(WorkoutTemplate(id = "t2"))

        viewModel.deleteTemplate("t1")

        assertEquals(listOf("t2"), repository.stored.map { it.id })
    }

    @Test
    fun `a failed save surfaces in templateError and keeps the draft`() {
        repository.failNext = IllegalStateException("offline")
        viewModel.updateDraftName("Push Day")
        viewModel.setDraftExercises(listOf("Bench Press"))

        viewModel.saveDraftTemplate()

        assertEquals("offline", viewModel.templateError.value)
        assertTrue(repository.stored.isEmpty())
        assertEquals("Push Day", viewModel.draftName.value)
        viewModel.clearTemplateError()
        assertNull(viewModel.templateError.value)
    }
}
