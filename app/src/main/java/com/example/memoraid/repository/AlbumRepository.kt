package com.example.memoraid.repository

import android.net.Uri
import com.example.memoraid.models.Album
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

    suspend fun loadAlbumDetails(albumId: String): Album? {
        return try {
            val document = firestoreCollection.document(albumId).get().await()
            document.toObject(Album::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveAlbumDetails(album: Album): Boolean {
        return try {
            firestoreCollection.document(album.id!!).set(album).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeImageFromFirestore(image: String): Boolean {
        return try {
            firestoreCollection.whereArrayContains("images", image).get().await().documents.forEach { document ->
                val updatedUris = (document["images"] as List<String>).filter { it != image }
                firestoreCollection.document(document.id).update("images", updatedUris).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeImageFromStorage(image: String): Boolean {
        return try {
            val fileReference = storage.getReferenceFromUrl(image)
            fileReference.delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadImageToStorage(image: Uri): String? {
        return try {
            val fileName = "${System.currentTimeMillis()}.jpg"
            val fileReference = storageReference.child(fileName)
            fileReference.putFile(image).await()
            fileReference.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkIfImageExistsInStorage(image: String): Boolean {
        return try {
            val fileReference = storage.getReferenceFromUrl(image)
            fileReference.downloadUrl.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}