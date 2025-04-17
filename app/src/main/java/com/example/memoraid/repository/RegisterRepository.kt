package com.example.memoraid.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val firestoreCollection get() = firestore.collection("users")

    suspend fun isUsernameUnique(username: String): Boolean {
        val result = firestoreCollection
            .whereEqualTo("username", username)
            .get()
            .await()
        return result.isEmpty
    }

    suspend fun isEmailUnique(email: String): Boolean {
        val result = firestoreCollection
            .whereEqualTo("email", email)
            .get()
            .await()
        return result.isEmpty
    }

//    suspend fun saveUserData(userId: String, data: Map<String, Any>) {
//        firestoreCollection.document(userId).set(data)
//    }
//
//    suspend fun uploadProfileImage(userId: String, imageUri: String) {
//        val storageRef = storage.reference.child("profile_images/$userId.jpg")
//        storageRef.putFile(imageUri.toUri()).await()
//    }
}