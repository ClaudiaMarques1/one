package app.theone.android.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(private val repository: DiaryRepository = DiaryRepository()) : ViewModel() {
    private val _diaryError = MutableStateFlow<String?>(null)
    val diaryError: StateFlow<String?> = _diaryError

    val entries: StateFlow<List<DiaryEntry>> = repository.observeEntries()
        .catch { e -> _diaryError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearDiaryError() {
        _diaryError.value = null
    }

    fun addEntry(
        mood: String,
        energyLevel: Int,
        sleepHours: Int,
        soreness: Map<String, Int>,
        reflection: String
    ) {
        val today = DiaryDateUtils.todayKey()
        viewModelScope.launch {
            runCatching {
                repository.addEntry(
                    DiaryEntry(
                        date = today,
                        mood = mood,
                        energyLevel = energyLevel,
                        sleepHours = sleepHours,
                        sorePartIds = soreness.keys.toList(),
                        soreness = soreness,
                        reflection = reflection
                    )
                )
            }.onFailure { _diaryError.value = it.message }
        }
    }

    fun updateEntry(
        id: String,
        date: String,
        mood: String,
        energyLevel: Int,
        sleepHours: Int,
        soreness: Map<String, Int>,
        reflection: String,
        createdAt: Long
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateEntry(
                    DiaryEntry(
                        id = id,
                        date = date,
                        mood = mood,
                        energyLevel = energyLevel,
                        sleepHours = sleepHours,
                        sorePartIds = soreness.keys.toList(),
                        soreness = soreness,
                        reflection = reflection,
                        createdAt = createdAt
                    )
                )
            }.onFailure { _diaryError.value = it.message }
        }
    }

    // Restores a deleted entry from a cached copy (delete-with-undo), keeping its original id.
    fun restoreEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            runCatching { repository.updateEntry(entry) }
                .onFailure { _diaryError.value = it.message }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(id) }
                .onFailure { _diaryError.value = it.message }
        }
    }
}
