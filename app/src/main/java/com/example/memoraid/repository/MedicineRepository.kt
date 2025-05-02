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

    suspend fun loadMedicine(date: String, userId: String): List<Medicine> {
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

    suspend fun addMedicine(medicine: Medicine): String? {
        val userId = requireUserId()

        val patientId = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            .getString("selectedPatient")

        medicine.isTaken = false
        if (patientId != null) {
            medicine.userId = patientId
        }

        return try {
            val docRef = firestoreCollection.add(medicine).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateMedicine(medicine: Medicine): Boolean {
        return try {
            if (medicine.id != null) {
                firestoreCollection.document(medicine.id!!).set(medicine).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMedicine(medicineId: String): Boolean {
        return try {
            firestoreCollection.document(medicineId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}