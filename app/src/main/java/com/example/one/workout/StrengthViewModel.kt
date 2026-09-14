package com.example.one.workout

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

class StrengthViewModel(private val repository: StrengthRepository = StrengthRepository()) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val sessions: StateFlow<List<StrengthSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentExercises = MutableStateFlow<List<StrengthExercise>>(emptyList())
    val currentExercises: StateFlow<List<StrengthExercise>> = _currentExercises

    fun addExercises(names: List<String>) {
        if (names.isEmpty()) return
        _currentExercises.value = _currentExercises.value + names.map { StrengthExercise(name = it) }
    }

    fun addSet(exerciseIndex: Int, set: StrengthSet) {
        _currentExercises.value = _currentExercises.value.toMutableList().also { list ->
            val exercise = list[exerciseIndex]
            list[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        }
    }

    fun removeExercise(index: Int) {
        _currentExercises.value = _currentExercises.value.toMutableList().apply { removeAt(index) }
    }

    fun saveSession() {
        val exercises = _currentExercises.value
        if (exercises.isEmpty()) return
        viewModelScope.launch {
            repository.addSession(StrengthSession(date = dateFormat.format(Date()), exercises = exercises))
            _currentExercises.value = emptyList()
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { repository.deleteSession(id) }
    }
}
