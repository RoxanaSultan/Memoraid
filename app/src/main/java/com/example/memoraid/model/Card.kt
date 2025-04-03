package com.example.memoraid.model

data class Card(
    val id: Int,
    val imageResId: Int,
    var isMatched: Boolean = false,
    var isFlipped: Boolean = false
)
