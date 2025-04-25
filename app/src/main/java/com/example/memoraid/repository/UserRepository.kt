package com.example.memoraid.repository

import android.net.Uri
import com.example.memoraid.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration

class UserRepository @Inject constructor(
    private val database: FirebaseFirestore,
    private val authentication: FirebaseAuth,
    private val storage: FirebaseStorage
) {
    private val firebaseCollection = database.collection("users")

    fun observeUser(userId: String, onUpdate: (User?) -> Unit): ListenerRegistration {
        return firebaseCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Listen failed", error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                onUpdate(user)
            }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val document = firebaseCollection.document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserData(uid: String): User? {
        return try {
            val doc = firebaseCollection.document(uid).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return authentication.currentUser
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = getCurrentUser()
        return if (user != null) {
            try {
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No user is logged in"))
        }
    }

    suspend fun getUserRole(): String? {
        val currentUser = getCurrentUser()

        return try {
            val snapshot = currentUser?.let {
                firebaseCollection
                    .document(it.uid)
                    .get()
                    .await()
            }

            val user = snapshot?.toObject(User::class.java)
            user?.role
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadAndSaveProfilePicture(uri: Uri): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        return try {
            val storageRef = storage.reference
                .child("profile_pictures/${userId}.jpg")

            storageRef.putFile(uri).await()

            val downloadUrl = storageRef.downloadUrl.await().toString()

            firebaseCollection.document(userId)
                .update("profilePictureUrl", downloadUrl)
                .await()

            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading and saving profile picture: ${e.message}", e)
            false
        }
    }

    suspend fun removeProfilePicture() {
        val userId = getCurrentUser()?.uid ?: return
        val storageRef = storage.reference.child("profile_pictures/${userId}.jpg")

        try {
            storageRef.delete().await()

            firebaseCollection.document(userId)
                .update("profilePictureUrl", null)
                .await()

            Log.d("UserRepository", "Profile picture removed successfully.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing profile picture: ${e.message}", e)
        }
    }

//
//    suspend fun uploadImageToStorage(uri: Uri): String? {
//        val userId = getCurrentUser()?.uid
//        return try {
//            val storageRef = storage.reference
//                .child("profile_pictures/${userId}.jpg")
//
//            storageRef.putFile(uri).await()
//
//            storageRef.downloadUrl.await().toString()
//        } catch (e: Exception) {
//            Log.e("UserRepository", "Error uploading image: ${e.message}", e)
//            null
//        }
//    }

    suspend fun deleteImageFromStorage(imageUrl: String) {
        try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting image: ${e.message}", e)
        }
    }

    suspend fun isUsernameUnique(username: String): Boolean {
        val result = firebaseCollection
            .whereEqualTo("username", username)
            .get()
            .await()
        return result.isEmpty
    }

    suspend fun isEmailUnique(email: String): Boolean {
        val result = firebaseCollection
            .whereEqualTo("email", email)
            .get()
            .await()
        return result.isEmpty
    }

    suspend fun saveUserDetails(userUpdates: Map<String, Any>): Boolean {
        val uid = getCurrentUser()?.uid ?: return false
        return try {
            firebaseCollection.document(uid)
                .update(userUpdates)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}