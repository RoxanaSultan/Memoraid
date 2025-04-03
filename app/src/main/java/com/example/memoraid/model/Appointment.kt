package com.example.memoraid.model

data class Appointment(
    var id: String = "",
    val name: String = "",
    val doctor: String? = null,
    val time: String = "",
    val location: String = "",
    val type: String = "",
    val date: String = "",
    val userId: String = "",
    var isCompleted: Boolean = false
)
