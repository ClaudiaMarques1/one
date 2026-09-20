package app.theone.android.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StrengthViewModel(
    private val repository: StrengthRepository = StrengthRepository(),
    private val templateRepository: WorkoutTemplateRepository = WorkoutTemplateRepository()
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val sessions: StateFlow<List<StrengthSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentExercises = MutableStateFlow<List<StrengthExercise>>(emptyList())
    val currentExercises: StateFlow<List<StrengthExercise>> = _currentExercises

    private val _activeTemplateId = MutableStateFlow("")
    private val _activeTemplateName = MutableStateFlow("")
    val activeTemplateName: StateFlow<String> = _activeTemplateName

    private val _sessionStartTimeMillis = MutableStateFlow<Long?>(null)
    val sessionStartTimeMillis: StateFlow<Long?> = _sessionStartTimeMillis

    fun startSessionFromTemplate(template: WorkoutTemplate) {
        _activeTemplateId.value = template.id
        _activeTemplateName.value = template.name
        _sessionStartTimeMillis.value = System.currentTimeMillis()
        _currentExercises.value = template.exercises.map { exercise ->
            StrengthExercise(
                name = exercise.name,
                sets = exercise.sets.map { StrengthSet(it.reps, it.weightKg) }
            )
        }
    }

    fun discardSession() {
        _activeTemplateId.value = ""
        _activeTemplateName.value = ""
        _sessionStartTimeMillis.value = null
        _currentExercises.value = emptyList()
    }

    fun setExercises(names: List<String>) {
        val existingByName = _currentExercises.value.associateBy { it.name }
        _currentExercises.value = names.map { name -> existingByName[name] ?: StrengthExercise(name = name) }
    }

    fun addSet(exerciseIndex: Int, set: StrengthSet) {
        _currentExercises.value = _currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, set: StrengthSet) {
        _currentExercises.value = _currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(
                sets = exercise.sets.toMutableList().apply { this[setIndex] = set }
            )
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _currentExercises.value = _currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(sets = exercise.sets.toMutableList().apply { removeAt(setIndex) })
        }
    }

    fun removeExercise(index: Int) {
        _currentExercises.value = _currentExercises.value.toMutableList().apply { removeAt(index) }
    }

    fun duplicateExercise(index: Int) {
        _currentExercises.value = _currentExercises.value.toMutableList().apply {
            add(index + 1, StrengthExercise(name = this[index].name))
        }
    }

    fun saveSession() {
        val exercises = _currentExercises.value
        if (exercises.isEmpty()) return
        val templateId = _activeTemplateId.value
        val templateName = _activeTemplateName.value
        viewModelScope.launch {
            repository.addSession(
                StrengthSession(date = dateFormat.format(Date()), templateName = templateName, exercises = exercises)
            )
            if (templateId.isNotEmpty()) {
                templateRepository.updateTemplateExercises(
                    templateId,
                    exercises.map { exercise ->
                        TemplateExercise(
                            name = exercise.name,
                            sets = exercise.sets.map { TemplateSet(it.reps, it.weightKg) }
                        )
                    }
                )
            }
            _currentExercises.value = emptyList()
            _activeTemplateId.value = ""
            _activeTemplateName.value = ""
            _sessionStartTimeMillis.value = null
        }
    }
}
