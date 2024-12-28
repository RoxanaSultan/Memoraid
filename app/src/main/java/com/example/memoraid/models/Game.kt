package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class Game(
    @DocumentId val id: String = "", // Auto-generated ID by Firestore
    val patientId: String = "", // Reference to the patient who is playing
    val name: String = "", // Name of the game (e.g., "Memory Puzzle")
    val progress: Float = 0.0f, // Progress in percentage (0.0 to 100.0)
    val lastAccessed: Date? = null, // Timestamp of the last time the game was played
    val highScore: Int = 0, // Highest score achieved in the game
    val status: String = "inProgress", // Game status (e.g., "inProgress", "completed")
)