package app.theone.android.exercise.backend

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSearchTest {
    private fun exercise(id: String, name: String, bodyPart: String, target: String = "") =
        Exercise(id, name, bodyPart, equipment = "barbell", target = target, emptyList(), emptyList(), imageUrl = "", gifUrl = "")

    private val bench = exercise("1", "Bench Press", "chest", target = "pectorals")
    private val fly = exercise("2", "Cable Fly", "chest", target = "pectorals")
    private val curl = exercise("3", "Barbell Curl", "upper arms", target = "biceps")
    private val squat = exercise("4", "Back Squat", "upper legs", target = "quads")
    private val all = listOf(bench, fly, curl, squat)

    @Test
    fun `categories are distinct and alphabetical`() {
        assertEquals(listOf("chest", "upper arms", "upper legs"), categoriesOf(listOf(squat, curl, bench, fly)))
    }

    @Test
    fun `no query shows the selected category`() {
        assertEquals(listOf(bench, fly), visibleExercises(all, query = "", category = "chest"))
        assertEquals(listOf(curl), visibleExercises(all, query = "  ", category = "upper arms"))
    }

    @Test
    fun `query searches name and target across all categories`() {
        assertEquals(listOf(bench, fly), visibleExercises(all, query = "PECT", category = "upper legs"))
        assertEquals(listOf(curl), visibleExercises(all, query = "curl", category = "chest"))
        assertEquals(emptyList<Exercise>(), visibleExercises(all, query = "deadlift", category = "chest"))
    }

    @Test
    fun `selected counts are per category`() {
        assertEquals(mapOf("chest" to 2, "upper legs" to 1), selectedCountByCategory(all, listOf("1", "2", "4")))
    }

    @Test
    fun `preselected ids follow the given name order and skip unknown names`() {
        assertEquals(listOf("4", "1"), preselectedIds(all, listOf("Back Squat", "Unknown", "Bench Press")))
    }

    @Test
    fun `selectedInOrder keeps selection order, not catalog order`() {
        assertEquals(listOf(squat, bench, curl), selectedInOrder(all, listOf("4", "1", "3")))
    }
}
