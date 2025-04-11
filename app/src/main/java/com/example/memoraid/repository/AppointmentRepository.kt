package com.example.memoraid.repository

import com.example.memoraid.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val firestoreCollection get() = firestore.collection("appointments")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw SecurityException("User not authenticated")
    }

    suspend fun loadAppointments(date: String): List<Appointment> {
        val userId = requireUserId()
        return firestoreCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val appointment = doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                appointment?.copy(isCompleted = doc.getBoolean("isCompleted") ?: false)
            }
    }
}