package app.theone.android.diary.journal.backend

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
