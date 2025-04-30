package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId

data class Appointment(
    @DocumentId val id: String = "",
    val name: String = "",
    val doctor: String? = null,
    val time: String = "",
    val location: String = "",
    val type: String = "",
    val date: String = "",
    var userId: String = "",
    var isCompleted: Boolean = false
)
