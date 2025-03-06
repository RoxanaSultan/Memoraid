package com.example.memoraid.models

data class Card(
    val id: Int, // Unique identifier
    val imageResId: Int, // Image resource ID
    var isMatched: Boolean = false, // If the card is matched
    var isFlipped: Boolean = false // If the card is flipped
)
