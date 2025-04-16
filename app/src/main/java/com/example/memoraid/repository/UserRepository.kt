package com.example.memoraid.repository

import com.example.memoraid.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(private val database: FirebaseFirestore) {
    private val firebaseCollection = database.collection("users")

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

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}