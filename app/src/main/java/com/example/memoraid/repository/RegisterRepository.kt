package com.example.memoraid.repository

import android.util.Log
import com.example.memoraid.models.Appointment
import com.example.memoraid.models.Patient
import com.example.memoraid.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
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

    suspend fun getPatients(): List<Patient> {
        return firestoreCollection
            .whereEqualTo("role", "patient")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val patient = doc.toObject(Patient::class.java)
                patient?.copy(
                    id = doc.id,
                    profilePicture = doc.getString("profilePictureUrl") ?: ""
                )
            }
    }

    suspend fun registerUser(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserDetails(
        uid: String,
        userInfo: HashMap<String, Any?>
    ): Boolean {
        return try {
            firestoreCollection.document(uid).set(userInfo).await()
            true
        } catch (e: Exception) {
            Log.e("RegisterRepo", "Error saving user details", e)
            false
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val documentSnapshot = firestoreCollection.document(userId).get().await()
            documentSnapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by ID", e)
            null
        }
    }

    suspend fun updatePatientCaretakers(patientId: String, updatedCaretakers: MutableList<String>?) {
        try {
            val patientRef = firestoreCollection.document(patientId)
            patientRef.update("caretakers", updatedCaretakers).await()
        } catch (e: Exception) {
            Log.e("RegisterRepo", "Error updating patient caretakers", e)
        }
    }
}