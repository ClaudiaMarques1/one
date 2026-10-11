package app.theone.android.diary.goals.backend

import kotlinx.coroutines.flow.Flow

/**
 * Goal storage. The app uses [FirestoreGoalRepository]; tests use an in-memory fake, so ViewModel
 * logic is unit-testable without Firebase.
 */
interface GoalRepository {
    /** Goals for the signed-in user, newest first; emits an empty list when signed out. */
    fun observeGoals(): Flow<List<Goal>>

    suspend fun addGoal(goal: Goal)

    /** Overwrites the goal with the same id. */
    suspend fun updateGoal(goal: Goal)

    suspend fun deleteGoal(id: String)
}
