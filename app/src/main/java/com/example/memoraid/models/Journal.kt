package com.example.memoraid.models

data class Journal(
    val id: String = "",
    val patientId: String = "", // Reference to the Patient ID
    val entryDate: Long = System.currentTimeMillis(), // Timestamp for the entry
    val text: String = "",
    val photos: List<String> = listOf() // List of photo URLs
)
