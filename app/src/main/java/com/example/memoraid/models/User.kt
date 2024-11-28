package com.example.memoraid.models
import java.sql.Timestamp

data class User(
    val id: String = "",
    val username: String = "",
    val emailOrPhone: String = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val role: String = "", // Either "patient" or "caretaker"
    val profilePictureUrl: String? = null,
    val birthday: Timestamp? = null // Optional: Store as "yyyy-MM-dd" or Timestamp
)
