package app.theone.android.workout.planner.backend

import kotlinx.coroutines.flow.Flow

/**
 * Training plan storage (#74). The app uses [FirestoreTrainingPlanRepository]; tests use an in-memory
 * fake. Both flows emit an empty list when signed out.
 */
interface TrainingPlanRepository {
    /** Every routine version, oldest [TrainingRoutine.effectiveFrom] first. */
    fun observeRoutines(): Flow<List<TrainingRoutine>>

    /** Overrides whose [DayOverride.sourceDate] is on or after [fromDate] (ISO), oldest first. */
    fun observeOverrides(fromDate: String): Flow<List<DayOverride>>

    /** Saves [routine] as the version starting on its `effectiveFrom` - the same day overwrites. */
    suspend fun saveRoutine(routine: TrainingRoutine)

    /** Saves [override] under its `sourceDate`, replacing any earlier change to that day. */
    suspend fun saveOverride(override: DayOverride)

    /** Drops the change to [sourceDate] (Reset / Remove in the day editor). */
    suspend fun deleteOverride(sourceDate: String)
}
