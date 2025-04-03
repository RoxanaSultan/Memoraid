package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId

data class Journal(
    @DocumentId val id: String = "",
    val userId: String = "",
    var entryDate: String = "",
    var title: String? = "",
    var text: String? = "",
    var imageUris: MutableList<String>? = null,
    var type: String = ""
)
