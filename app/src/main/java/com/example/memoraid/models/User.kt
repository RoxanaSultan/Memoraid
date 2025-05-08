package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val role: String = "",
    val profilePictureUrl: String? = null,
    val birthdate: String? = null,
    val patients: List<String>? = null,
    val caretakers: List<String>? = null,
    val selectedPatient: String? = null,
    val location: GeoPoint? = null
)