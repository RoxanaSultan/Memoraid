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

    suspend fun loadAppointments(date: String, userId: String): List<Appointment> {
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

    suspend fun createAppointment(appointment: Appointment): String? {
        val userId = requireUserId()

        val patientId = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            .getString("selectedPatient")

        appointment.isCompleted = false
        if (patientId != null) {
            appointment.userId = patientId
        }

        return try {
            val docRef = firestoreCollection.add(appointment).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateAppointment(appointment: Appointment): Boolean {
        return try {
            if (appointment.id != null) {
                firestoreCollection.document(appointment.id!!).set(appointment).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAppointment(appointmentId: String): Boolean {
        return try {
            firestoreCollection.document(appointmentId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}