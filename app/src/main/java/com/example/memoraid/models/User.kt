package com.example.memoraid.models

data class User(
    val username: String = "",
    val emailPhone: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val profilePictureUrl: String = "",
    val role: String = ""
)
