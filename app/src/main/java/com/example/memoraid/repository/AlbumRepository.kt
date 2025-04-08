package com.example.memoraid.repository

import com.example.memoraid.models.Album
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val firestoreCollection get() = firestore.collection("albums")
    private val storageReference  get() = storage.reference.child("album_images")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw SecurityException("User not authenticated")
    }

    suspend fun loadAlbums(): List<Album> {
        val userId = requireUserId()
        return firestoreCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Album::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun getAlbum(albumId: String): Album? {
        val userId = requireUserId()
        return firestoreCollection
            .document(albumId)
            .get()
            .await()
            .takeIf { doc ->
                doc.exists() && doc.getString("userId") == userId
            }
            ?.toObject(Album::class.java)
            ?.copy(id = albumId)
    }

    suspend fun createAlbum(albumType: String): String {
        val userId = requireUserId()
        val newAlbum = Album(
            userId = userId,
            createdAt = Timestamp.now(),
            title = "Untitled",
            description = "No description",
            images = listOf<String>(),
            type = albumType,
            updatedAt = Timestamp.now()
        )

        val docRef = firestoreCollection.document()
        docRef.set(newAlbum).await()
        return docRef.id
    }

    suspend fun deleteAlbum(albumId: String): Boolean {
        val document = firestoreCollection.document(albumId).get().await()
        val images = document.get("images") as? List<String> ?: emptyList()

        for (image in images) {
            try {
                storage.getReferenceFromUrl(image).delete().await()
            } catch (_: Exception) {
            }
        }

        firestoreCollection.document(albumId).delete().await()
        return true
    }
}