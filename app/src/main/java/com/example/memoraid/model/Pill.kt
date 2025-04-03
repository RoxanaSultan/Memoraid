package com.example.memoraid.model

data class Pill(
    var id: String = "",
    val name: String = "",
    val time: String = "",
    val date: String = "",
    val dose: String = "",
    val userId: String = "",
    var isTaken: Boolean = false,
    val note: String? = null
)