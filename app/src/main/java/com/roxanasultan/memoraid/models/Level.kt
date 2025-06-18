package com.roxanasultan.memoraid.models

data class Level(
    val bestTime: Long = 0,
    val lastPlayedGames: List<LastPlayedGame> = emptyList(),
    val leastMoves: Int = 0,
    val level: String = "",
    val totalLevelScore: Int = 0
)
