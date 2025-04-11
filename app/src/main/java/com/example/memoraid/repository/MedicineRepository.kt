package com.example.memoraid.repository

import com.example.memoraid.models.Medicine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val firestoreCollection get() = firestore.collection("medicine")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw SecurityException("User not authenticated")
    }

    suspend fun loadMedicine(date: String): List<Medicine> {
        val userId = requireUserId()
        return firestoreCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val medicine = doc.toObject(Medicine::class.java)?.copy(id = doc.id)
                medicine?.copy(isTaken = doc.getBoolean("isTaken") ?: false)
            }
    }
}