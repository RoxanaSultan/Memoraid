package com.example.memoraid.models

// Caretaker model inherits from User
data class Caretaker(
    val assignedPatients: List<String> = listOf() // List of patient IDs
) : User()
