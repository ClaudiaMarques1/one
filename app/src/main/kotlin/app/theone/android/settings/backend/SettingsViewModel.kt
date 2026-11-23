package app.theone.android.settings.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.meals.backend.MealSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The repository needs a Context, so there's no default argument - screens build this through a
// viewModelFactory with the application context (`settingsViewModel()` in settings/frontend).
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _settingsError = MutableStateFlow<String?>(null)
    val settingsError: StateFlow<String?> = _settingsError

    val vibrateOnRestEnd: StateFlow<Boolean> = repository.observeVibrateOnRestEnd()
        .catch { e -> _settingsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_VIBRATE_ON_REST_END)

    fun setVibrateOnRestEnd(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setVibrateOnRestEnd(enabled) }
                .onFailure { _settingsError.value = it.message }
        }
    }

    val dndDuringWorkout: StateFlow<Boolean> = repository.observeDndDuringWorkout()
        .catch { e -> _settingsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDndDuringWorkout(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setDndDuringWorkout(enabled) }
                .onFailure { _settingsError.value = it.message }
        }
    }

    val themeMode: StateFlow<ThemeMode> = repository.observeThemeMode()
        .catch { e -> _settingsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_THEME_MODE)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            runCatching { repository.setThemeMode(mode) }
                .onFailure { _settingsError.value = it.message }
        }
    }

    val mealSettings: StateFlow<MealSettings> = repository.observeMealSettings()
        .catch { e -> _settingsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealSettings())

    val weekStart: StateFlow<WeekStart> = repository.observeWeekStart()
        .catch { e -> _settingsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekStart.fromLocale())

    fun setWeekStart(weekStart: WeekStart) {
        viewModelScope.launch {
            runCatching { repository.setWeekStart(weekStart) }
                .onFailure { _settingsError.value = it.message }
        }
    }

    fun setMealSettings(settings: MealSettings) {
        viewModelScope.launch {
            runCatching { repository.setMealSettings(settings) }
                .onFailure { _settingsError.value = it.message }
        }
    }

    fun clearSettingsError() {
        _settingsError.value = null
    }
}
