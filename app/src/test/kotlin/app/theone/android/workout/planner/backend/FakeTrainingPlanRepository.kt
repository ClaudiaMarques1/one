package app.theone.android.workout.planner.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [TrainingPlanRepository] for tests (phases 2-4 of the planner). Mirrors the Firestore
 * contract: routines oldest `effectiveFrom` first, one per start date; overrides one per `sourceDate`,
 * filtered from a date and oldest first.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeTrainingPlanRepository(
    routines: List<TrainingRoutine> = emptyList(),
    overrides: List<DayOverride> = emptyList()
) : TrainingPlanRepository {
    private val routineState = MutableStateFlow(routines.associateBy { it.effectiveFrom })
    private val overrideState = MutableStateFlow(overrides.associateBy { it.sourceDate })

    var failNext: Throwable? = null

    val storedRoutines: List<TrainingRoutine> get() = routineState.value.values.sortedBy { it.effectiveFrom }
    val storedOverrides: List<DayOverride> get() = overrideState.value.values.sortedBy { it.sourceDate }

    override fun observeRoutines(): Flow<List<TrainingRoutine>> =
        routineState.map { map -> map.values.sortedBy { it.effectiveFrom } }

    override fun observeOverrides(fromDate: String): Flow<List<DayOverride>> =
        overrideState.map { map -> map.values.filter { it.sourceDate >= fromDate }.sortedBy { it.sourceDate } }

    override suspend fun saveRoutine(routine: TrainingRoutine) {
        throwIfFailing()
        routineState.value = routineState.value + (routine.effectiveFrom to routine)
    }

    override suspend fun saveOverride(override: DayOverride) {
        throwIfFailing()
        overrideState.value = overrideState.value + (override.sourceDate to override)
    }

    override suspend fun deleteOverride(sourceDate: String) {
        throwIfFailing()
        overrideState.value = overrideState.value - sourceDate
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
