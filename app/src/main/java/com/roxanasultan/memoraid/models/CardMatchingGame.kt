package com.roxanasultan.memoraid.models

import com.google.firebase.firestore.DocumentId

data class CardMatchingGame(
    @DocumentId val id: String = "",
    val levels: List<Level> = emptyList(),
    val totalScore: Int = 0,
    val userId: String = ""
)
