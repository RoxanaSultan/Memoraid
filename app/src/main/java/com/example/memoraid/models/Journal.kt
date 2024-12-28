package com.example.memoraid.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Journal(
    @DocumentId val id: String = "", // You can remove this if using auto-generated IDs
    val patientId: String = "", // Reference to the Patient ID
    @ServerTimestamp val entryDate: Date? = null, // Firestore server timestamp
    val text: String? = "", // Optional text for the entry
    val photos: List<String> = listOf() // List of photo URLs
)
