package app.theone.android.exercise.backend

// What the exercise picker shows and returns - pure, so it's unit-tested apart from the dialog.

/** The rail's categories (body parts), alphabetical. */
fun categoriesOf(exercises: List<Exercise>): List<String> = exercises.map { it.bodyPart }.distinct().sorted()

/**
 * A non-blank [query] searches name and target across every category (case-insensitive);
 * otherwise the list is [category]'s exercises.
 */
fun visibleExercises(exercises: List<Exercise>, query: String, category: String?): List<Exercise> {
    val q = query.trim()
    return if (q.isNotEmpty()) {
        exercises.filter { it.name.contains(q, ignoreCase = true) || it.target.contains(q, ignoreCase = true) }
    } else {
        exercises.filter { it.bodyPart == category }
    }
}

/** Count badges on the rail: selected exercises per category. */
fun selectedCountByCategory(exercises: List<Exercise>, selectedIds: Collection<String>): Map<String, Int> =
    exercises.filter { it.id in selectedIds }.groupingBy { it.bodyPart }.eachCount()

/** Ids of the exercises already in the template or workout, in that order. */
fun preselectedIds(exercises: List<Exercise>, names: Collection<String>): List<String> {
    val idByName = exercises.associate { it.name to it.id }
    return names.mapNotNull { idByName[it] }.distinct()
}

/** The confirmed list, in selection order - so exercises already in a template keep their order. */
fun selectedInOrder(exercises: List<Exercise>, selectedIds: List<String>): List<Exercise> {
    val byId = exercises.associateBy { it.id }
    return selectedIds.mapNotNull { byId[it] }
}
