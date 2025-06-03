package com.roxanasultan.memoraid.repositories

import com.roxanasultan.memoraid.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
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

    fun observeAppointments(date: String, userId: String, onDataChange: (List<Appointment>) -> Unit): ListenerRegistration {
        return firestore.collection("appointments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val appointments = snapshot.toObjects(Appointment::class.java)
                    onDataChange(appointments)
                } else {
                    onDataChange(emptyList())
                }
            }
    }

    suspend fun loadAppointments(date: String, userId: String): List<Appointment> {
        val allAppointments = firestoreCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val appointment = doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                appointment?.copy(completed = doc.getBoolean("completed") ?: false)
            }
        val selectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(date)
        return allAppointments.filter { it.isActiveOnDate(selectedDate) }
    }

    suspend fun createAppointment(appointment: Appointment): String? {
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