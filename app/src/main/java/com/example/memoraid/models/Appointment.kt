package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId

data class Appointment(
    @DocumentId val id: String = "",
    val name: String = "",
    val doctor: String? = null,
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val type: String = "",
    var userId: String = "",
    var completed: Boolean = false
)
