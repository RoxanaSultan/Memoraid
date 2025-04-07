package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
open class User(
    @DocumentId val id: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val role: String = "",
    val profilePictureUrl: String? = null,
    val birthdate: String? = null
)