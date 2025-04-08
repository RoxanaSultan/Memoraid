package com.example.memoraid.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue

data class Album(
    @DocumentId val id: String = "",
    val userId: String = "",
    var title: String? = "",
    var description: String? = "",
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),
    var images: List<String>? = null,
    var type: String = ""
)
