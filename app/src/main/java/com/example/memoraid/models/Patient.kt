package com.example.memoraid.models

data class Patient(
    val medicalHistory: String? = null,
    val assignedCaretakers: List<String> = listOf(), // List of caretaker IDs
    val location: Location? = null // Optional: Current location
) : User()
