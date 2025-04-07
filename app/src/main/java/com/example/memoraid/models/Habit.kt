package com.example.memoraid.models

data class Habit (
    var id: String = "",
    val name: String = "",
    val userId: String = "",
    var isChecked: Boolean = false
)