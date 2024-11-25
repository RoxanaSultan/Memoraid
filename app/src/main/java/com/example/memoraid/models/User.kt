package com.example.memoraid.models

data class User(
    val userId: String = "",
    val username: String = "",
    val emailPhone: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthday: String = "",
    val profilePictureUrl: String = "",
    val role: String = ""
)
