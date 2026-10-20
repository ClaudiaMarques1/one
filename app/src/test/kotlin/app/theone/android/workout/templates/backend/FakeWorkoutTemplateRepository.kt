package app.theone.android.workout.templates.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [WorkoutTemplateRepository] for tests. Mirrors the Firestore implementation's contract:
 * templates come back newest `createdAt` first, `addTemplate` assigns an id, `updateTemplate`
 * overwrites by id, `updateTemplateExercises` replaces only the exercises.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeWorkoutTemplateRepository(initial: List<WorkoutTemplate> = emptyList()) : WorkoutTemplateRepository {
    private val templates = MutableStateFlow(initial)
    private var nextId = 1

    var failNext: Throwable? = null

    /** Current stored templates, in insertion order. */
    val stored: List<WorkoutTemplate> get() = templates.value

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        templates.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun addTemplate(template: WorkoutTemplate) {
        throwIfFailing()
        templates.value = templates.value + template.copy(id = "fake-${nextId++}")
    }

    override suspend fun updateTemplate(template: WorkoutTemplate) {
        throwIfFailing()
        templates.value = templates.value.filterNot { it.id == template.id } + template
    }

    override suspend fun updateTemplateExercises(templateId: String, exercises: List<TemplateExercise>) {
        throwIfFailing()
        templates.value = templates.value.map { if (it.id == templateId) it.copy(exercises = exercises) else it }
    }

    override suspend fun deleteTemplate(id: String) {
        throwIfFailing()
        templates.value = templates.value.filterNot { it.id == id }
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
