package com.example.memoraid.models

data class Medicine(
    var id: String = "",
    val name: String = "",
    val time: String = "",
    val date: String = "",
    val dose: String = "",
    var userId: String = "",
    var isTaken: Boolean = false,
    val note: String? = null
)