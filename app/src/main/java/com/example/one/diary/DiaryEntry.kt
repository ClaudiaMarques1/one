package com.example.one.diary

data class DiaryEntry(
    val id: String = "",
    val date: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
