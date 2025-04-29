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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getPatient(caretakerId: String): User? {
        return try {
            val caretakerDocument = firebaseCollection.document(caretakerId).get().await()
            val caretaker = caretakerDocument.toObject(User::class.java)
            val selectedPatientId = caretaker?.selectedPatient

            if (selectedPatientId != null) {
                getUser(selectedPatientId)
            } else {
                null
            }
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

    suspend fun getCurrentPatient(): User? {
        val currentUser = authentication.currentUser ?: return null
        val userSnapshot = firebaseCollection.document(currentUser.uid).get().await()
        val selectedPatientId = userSnapshot.getString("selectedPatient") ?: return null

        val patientSnapshot = firebaseCollection.document(selectedPatientId).get().await()
        return patientSnapshot.toObject(User::class.java)
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
        val userId = getCurrentUser()?.uid ?: return null
        return try {
            val snapshot = firebaseCollection
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            user?.role
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadAndSaveProfilePicture(uri: Uri, userId: String): Boolean {
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

    suspend fun removeProfilePicture(userId: String) {
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

    suspend fun saveUserDetails(userUpdates: Map<String, Any>, userId: String): Boolean {
        return try {
            firebaseCollection.document(userId)
                .update(userUpdates)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAssignedPatients(): List<User> {
        val caretakerId = getCurrentUser()?.uid ?: return emptyList()

        return withContext(Dispatchers.IO) {
            val caretakerDoc = firebaseCollection
                .document(caretakerId)
                .get()
                .await()

            val patientIds = caretakerDoc.get("patients") as? List<String> ?: emptyList()
            if (patientIds.isEmpty()) return@withContext emptyList()

            val snapshot = firebaseCollection
                .whereIn(FieldPath.documentId(), patientIds)
                .get()
                .await()

            return@withContext snapshot.toObjects(User::class.java)
        }
    }

    suspend fun findPatientByQuery(query: String): User? {
        return withContext(Dispatchers.IO) {
            val snapshot = firebaseCollection
                .whereEqualTo("role", "patient")
                .get()
                .await()

            val allPatients = snapshot.toObjects(User::class.java)
            val lowerQuery = query.trim().lowercase()

            return@withContext allPatients.firstOrNull { user ->
                user.username?.trim()?.lowercase() == lowerQuery ||
                        user.firstName?.trim()?.lowercase() == lowerQuery ||
                        user.lastName?.trim()?.lowercase() == lowerQuery ||
                        user.phoneNumber?.trim()?.lowercase() == lowerQuery ||
                        user.email?.trim()?.lowercase() == lowerQuery
            }
        }
    }

    fun addPatientToCurrentCaretaker(patient: User) {
        val caretakerId = getCurrentUser()?.uid ?: return

        val caretakerRef = firebaseCollection.document(caretakerId)
        val patientRef = firebaseCollection.document(patient.id)

        caretakerRef.update("patients", FieldValue.arrayUnion(patient.id))

        patientRef.update("caretakers", FieldValue.arrayUnion(caretakerId))
    }

    fun removePatientFromCaretaker(patientId: String, onComplete: (Boolean) -> Unit) {
        val caretakerId = getCurrentUser()?.uid ?: run {
            onComplete(false)
            return
        }

        val caretakerRef = firebaseCollection.document(caretakerId)
        val patientRef = firebaseCollection.document(patientId)

        caretakerRef.update("patients", FieldValue.arrayRemove(patientId))
            .addOnSuccessListener {
                patientRef.update("caretakers", FieldValue.arrayRemove(caretakerId))
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}