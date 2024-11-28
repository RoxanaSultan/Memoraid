package com.example.memoraid.models
data class PhotoAlbum(
    val id: String = "",
    val patientId: String = "", // Reference to the Patient ID
    val photos: List<String> = listOf(), // List of photo URLs
    val createdAt: Long = System.currentTimeMillis() // Timestamp for creation
)
