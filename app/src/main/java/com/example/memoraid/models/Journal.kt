package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Journal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryDate: String = "",
    var title: String? = "",
    var text: String? = "",
    var imageUris: MutableList<String>? = null
)
