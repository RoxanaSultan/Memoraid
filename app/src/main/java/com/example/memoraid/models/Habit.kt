package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId

data class Habit(
    @DocumentId val id: String = "",
    val name: String = "",
    var userId: String = "",
    val checkedDates: ArrayList<String> = arrayListOf(),
)