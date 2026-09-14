package com.example.one.diary

data class DiaryEntry(
    val id: String = "",
    val date: String = "",
    val mood: String = "",
    val energyLevel: Int = 5,
    val sleepHours: Int = 5,
    val sorePartIds: List<String> = emptyList(),
    val reflection: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

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
