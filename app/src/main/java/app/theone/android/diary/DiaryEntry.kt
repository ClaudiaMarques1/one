package app.theone.android.diary

data class DiaryEntry(
    val id: String = "",
    val date: String = "",
    val mood: String = "",
    val energyLevel: Int = 5,
    val sleepHours: Int = 5,
    val sorePartIds: List<String> = emptyList(),
    // partId -> 1 Mild, 2 Sore, 3 Very sore. New field (see design_handoff_diary_journal); sorePartIds
    // is kept so the Calendar and other existing readers keep working off a flat id list.
    val soreness: Map<String, Int> = emptyMap(),
    val reflection: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Entries saved before `soreness` existed only have sorePartIds - treat those ids as level 2 (Sore).
// An extension, not a class member: Firestore serializes every public getter on the model, so a
// member property would be written into each document as a redundant `effectiveSoreness` field.
val DiaryEntry.effectiveSoreness: Map<String, Int>
    get() = soreness.ifEmpty { sorePartIds.associateWith { 2 } }

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
