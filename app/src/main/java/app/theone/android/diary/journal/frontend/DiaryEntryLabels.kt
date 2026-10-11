package app.theone.android.diary.journal.frontend

// Display text for diary entry fields: body part names, soreness levels and moods.

val bodyPartLabels = mapOf(
    "shoulders" to "Shoulders",
    "chest" to "Chest",
    "arms" to "Arms",
    "abs" to "Abs",
    "quads" to "Quads",
    "calves" to "Calves",
    "back" to "Back",
    "glutes" to "Glutes",
    "hamstrings" to "Hamstrings"
)

// Display order for sore-part chips (diary entry cards and the composer's marked-parts list).
val bodyPartOrder = listOf("shoulders", "chest", "arms", "abs", "back", "glutes", "quads", "hamstrings", "calves")

val sorenessLevelLabels = listOf("Mild", "Sore", "Very sore") // index 0..2 for level 1..3

val moodOptions = listOf(
    "😄" to "Great",
    "🙂" to "Good",
    "😐" to "Okay",
    "😣" to "Sore",
    "😤" to "Frustrated",
    "💪" to "Strong",
    "😴" to "Tired"
)

private val moodLabelByEmoji = moodOptions.toMap()

fun moodLabel(emoji: String): String = moodLabelByEmoji[emoji] ?: ""
