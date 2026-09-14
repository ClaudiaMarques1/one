package com.example.one.workout

data class StrengthSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0
)

data class StrengthExercise(
    val name: String = "",
    val sets: List<StrengthSet> = emptyList()
)

data class StrengthSession(
    val id: String = "",
    val date: String = "",
    val exercises: List<StrengthExercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
