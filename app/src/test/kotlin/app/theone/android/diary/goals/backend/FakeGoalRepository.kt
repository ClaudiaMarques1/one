package app.theone.android.diary.goals.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [GoalRepository] for tests. Mirrors the Firestore implementation's contract: goals
 * come back newest `createdAt` first, `addGoal` assigns an id, `updateGoal` overwrites by id.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeGoalRepository(initial: List<Goal> = emptyList()) : GoalRepository {
    private val goals = MutableStateFlow(initial)
    private var nextId = 1

    var failNext: Throwable? = null

    /** Current stored goals, in insertion order. */
    val stored: List<Goal> get() = goals.value

    override fun observeGoals(): Flow<List<Goal>> =
        goals.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun addGoal(goal: Goal) {
        throwIfFailing()
        goals.value = goals.value + goal.copy(id = "fake-${nextId++}")
    }

    override suspend fun updateGoal(goal: Goal) {
        throwIfFailing()
        goals.value = goals.value.filterNot { it.id == goal.id } + goal
    }

    override suspend fun deleteGoal(id: String) {
        throwIfFailing()
        goals.value = goals.value.filterNot { it.id == id }
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
