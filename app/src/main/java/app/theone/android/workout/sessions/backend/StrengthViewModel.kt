package app.theone.android.workout.sessions.backend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StrengthViewModel(
    private val repository: StrengthRepository = FirestoreStrengthRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    private val _workoutError = MutableStateFlow<String?>(null)
    val workoutError: StateFlow<String?> = _workoutError

    val sessions: StateFlow<List<StrengthSession>> = repository.observeSessions()
        .catch { e -> _workoutError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearWorkoutError() {
        _workoutError.value = null
    }

    // Backed by SavedStateHandle (not plain MutableStateFlow) so an in-progress session
    // survives process death - see StrengthSet/StrengthExercise's Serializable conformance.
    val currentExercises: StateFlow<List<StrengthExercise>> =
        savedStateHandle.getStateFlow(KEY_CURRENT_EXERCISES, emptyList())

    private val activeTemplateId: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_ACTIVE_TEMPLATE_ID, "")
    val activeTemplateName: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_ACTIVE_TEMPLATE_NAME, "")

    val sessionStartTimeMillis: StateFlow<Long?> =
        savedStateHandle.getStateFlow(KEY_SESSION_START_TIME, null)

    fun startSessionFromTemplate(template: WorkoutTemplate) {
        savedStateHandle[KEY_ACTIVE_TEMPLATE_ID] = template.id
        savedStateHandle[KEY_ACTIVE_TEMPLATE_NAME] = template.name
        savedStateHandle[KEY_SESSION_START_TIME] = System.currentTimeMillis()
        savedStateHandle[KEY_CURRENT_EXERCISES] = template.exercises.map { exercise ->
            StrengthExercise(
                name = exercise.name,
                sets = exercise.sets.map { StrengthSet(it.reps, it.weightKg) }
            )
        }
    }

    fun discardSession() {
        savedStateHandle[KEY_ACTIVE_TEMPLATE_ID] = ""
        savedStateHandle[KEY_ACTIVE_TEMPLATE_NAME] = ""
        savedStateHandle[KEY_SESSION_START_TIME] = null
        savedStateHandle[KEY_CURRENT_EXERCISES] = emptyList<StrengthExercise>()
    }

    fun setExercises(names: List<String>) {
        val existingByName = currentExercises.value.associateBy { it.name }
        savedStateHandle[KEY_CURRENT_EXERCISES] =
            names.map { name -> existingByName[name] ?: StrengthExercise(name = name) }
    }

    fun addSet(exerciseIndex: Int, set: StrengthSet) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, set: StrengthSet) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(
                sets = exercise.sets.toMutableList().apply { this[setIndex] = set }
            )
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(sets = exercise.sets.toMutableList().apply { removeAt(setIndex) })
        }
    }

    fun removeExercise(index: Int) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = currentExercises.value.toMutableList().apply { removeAt(index) }
    }

    fun duplicateExercise(index: Int) {
        savedStateHandle[KEY_CURRENT_EXERCISES] = currentExercises.value.toMutableList().apply {
            add(index + 1, StrengthExercise(name = this[index].name))
        }
    }

    fun saveSession() {
        val exercises = currentExercises.value
        if (exercises.isEmpty()) return
        val templateId = activeTemplateId.value
        val templateName = activeTemplateName.value
        viewModelScope.launch {
            runCatching {
                repository.addSessionAndUpdateTemplate(
                    session = StrengthSession(date = LocalDate.now().toString(), templateName = templateName, exercises = exercises),
                    templateId = templateId.ifEmpty { null },
                    templateExercises = exercises.takeIf { templateId.isNotEmpty() }?.map { exercise ->
                        TemplateExercise(
                            name = exercise.name,
                            sets = exercise.sets.map { TemplateSet(it.reps, it.weightKg) }
                        )
                    }
                )
            }.onFailure { _workoutError.value = it.message }
                .onSuccess {
                    savedStateHandle[KEY_CURRENT_EXERCISES] = emptyList<StrengthExercise>()
                    savedStateHandle[KEY_ACTIVE_TEMPLATE_ID] = ""
                    savedStateHandle[KEY_ACTIVE_TEMPLATE_NAME] = ""
                    savedStateHandle[KEY_SESSION_START_TIME] = null
                }
        }
    }

    private companion object {
        const val KEY_CURRENT_EXERCISES = "currentExercises"
        const val KEY_ACTIVE_TEMPLATE_ID = "activeTemplateId"
        const val KEY_ACTIVE_TEMPLATE_NAME = "activeTemplateName"
        const val KEY_SESSION_START_TIME = "sessionStartTimeMillis"
    }
}
