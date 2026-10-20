package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class StrengthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStrengthRepository()

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { StrengthViewModel(repository = repository, savedStateHandle = SavedStateHandle()) }

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

    @Test
    fun `sessions exposes the repository's sessions newest first`() = runTest {
        repository.addSession(StrengthSession(templateName = "old", createdAt = 1L))
        repository.addSession(StrengthSession(templateName = "new", createdAt = 2L))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.sessions.collect {} }

        assertEquals(listOf("new", "old"), viewModel.sessions.value.map { it.templateName })
    }

    @Test
    fun `saveSession from a template saves today's session, carries sets back, and clears state`() {
        viewModel.startSessionFromTemplate(
            WorkoutTemplate(id = "t1", name = "Push Day", exercises = listOf(TemplateExercise(name = "Bench Press")))
        )
        viewModel.addSet(0, StrengthSet(reps = 8, weightKg = 60.0))

        viewModel.saveSession()

        val saved = repository.stored.single()
        assertEquals(LocalDate.now().toString(), saved.date)
        assertEquals("Push Day", saved.templateName)
        assertEquals(listOf(StrengthSet(reps = 8, weightKg = 60.0)), saved.exercises.single().sets)
        val (templateId, carried) = repository.templateUpdates.single()
        assertEquals("t1", templateId)
        assertEquals(listOf(TemplateExercise(name = "Bench Press", sets = listOf(TemplateSet(reps = 8, weightKg = 60.0)))), carried)
        assertTrue(viewModel.currentExercises.value.isEmpty())
        assertEquals("", viewModel.activeTemplateName.value)
        assertNull(viewModel.sessionStartTimeMillis.value)
    }

    @Test
    fun `saveSession without a template saves the session and updates no template`() {
        viewModel.setExercises(listOf("Squat"))
        viewModel.addSet(0, StrengthSet(reps = 5, weightKg = 100.0))

        viewModel.saveSession()

        assertEquals("Squat", repository.stored.single().exercises.single().name)
        assertTrue(repository.templateUpdates.isEmpty())
    }

    @Test
    fun `saveSession does nothing when no exercises are logged`() {
        viewModel.startSessionFromTemplate(WorkoutTemplate(id = "t1", name = "Push Day"))

        viewModel.saveSession()

        assertTrue(repository.stored.isEmpty())
        assertEquals("Push Day", viewModel.activeTemplateName.value)
    }

    @Test
    fun `a failed save surfaces in workoutError and keeps the session`() {
        repository.failNext = IllegalStateException("offline")
        viewModel.startSessionFromTemplate(
            WorkoutTemplate(id = "t1", name = "Push Day", exercises = listOf(TemplateExercise(name = "Bench Press")))
        )

        viewModel.saveSession()

        assertEquals("offline", viewModel.workoutError.value)
        assertTrue(repository.stored.isEmpty())
        assertEquals("Push Day", viewModel.activeTemplateName.value)
        assertEquals(listOf("Bench Press"), viewModel.currentExercises.value.map { it.name })
        viewModel.clearWorkoutError()
        assertNull(viewModel.workoutError.value)
    }
}
