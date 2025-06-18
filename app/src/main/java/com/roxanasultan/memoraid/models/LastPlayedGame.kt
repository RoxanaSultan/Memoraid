package com.roxanasultan.memoraid.models

import java.util.Date

data class LastPlayedGame(
    val date: Date = Date(),
    val moves: Int = 0,
    val time: Long = 0,
    val score: Int = 0
)