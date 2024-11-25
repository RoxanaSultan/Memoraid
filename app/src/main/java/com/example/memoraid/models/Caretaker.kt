package com.example.memoraid.models

data class Caretaker(
    val caretakerId: String = "",
    val userId: String = "",
    val assignedPatients: List<String> = listOf()
)
