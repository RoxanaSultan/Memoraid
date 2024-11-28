package com.example.memoraid.models

data class Caretaker(
    val caretakerId: String = "",
    val assignedPatients: List<String> = listOf()
)
