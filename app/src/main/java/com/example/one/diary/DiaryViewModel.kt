package com.example.one.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryViewModel(private val repository: DiaryRepository = DiaryRepository()) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val entries: StateFlow<List<DiaryEntry>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(
        mood: String,
        energyLevel: Int,
        sleepHours: Int,
        sorePartIds: List<String>,
        reflection: String
    ) {
        val today = dateFormat.format(Date())
        viewModelScope.launch {
            repository.addEntry(
                DiaryEntry(
                    date = today,
                    mood = mood,
                    energyLevel = energyLevel,
                    sleepHours = sleepHours,
                    sorePartIds = sorePartIds,
                    reflection = reflection
                )
            )
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }
}
