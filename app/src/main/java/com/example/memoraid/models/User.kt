package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.sql.Timestamp

@IgnoreExtraProperties
open class User(
    @DocumentId val id: String = "", // Firestore will auto-generate this
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val role: String = "", // "patient" or "caretaker"
    val profilePictureUrl: String? = null,
    val birthday: String? = null // Optional: Store as "yyyy-MM-dd" or Timestamp
)