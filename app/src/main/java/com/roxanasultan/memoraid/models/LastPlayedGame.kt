package com.roxanasultan.memoraid.models

import java.util.Date

data class LastPlayedGame(
    val date: Date,
    val moves: Int = 0,
    val time: Int = 0,
    val score: Int = 0
)