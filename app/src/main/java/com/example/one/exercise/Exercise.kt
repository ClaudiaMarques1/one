package com.example.one.exercise

data class Exercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val imageUrl: String,
    val gifUrl: String
)

const val EXERCISE_MEDIA_ATTRIBUTION = "© Gym visual — https://gymvisual.com/"
