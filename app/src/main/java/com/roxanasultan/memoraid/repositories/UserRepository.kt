package com.roxanasultan.memoraid.repositories

import android.net.Uri
import com.roxanasultan.memoraid.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    fun logout() {
        FirebaseAuth.getInstance().signOut()
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

    fun getLastRouteLocation(userId: String, onResult: (GeoPoint?) -> Unit) {
        firebaseCollection.document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val currentRoute = doc.get("currentRoute") as? List<Map<String, Any>>
                    if (!currentRoute.isNullOrEmpty()) {
                        val lastEntry = currentRoute.last()
                        val location = lastEntry["location"] as? GeoPoint
                        onResult(location)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getPatientLocationFlow(patientId: String): Flow<GeoPoint?> = callbackFlow {
        val listenerRegistration = firebaseCollection.document(patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val location = snapshot?.getGeoPoint("location")
                trySend(location).isSuccess
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun updatePatientLocation(userId: String, lat: Double, lon: Double) {
        val locationMap = mapOf("latitude" to lat, "longitude" to lon)
        val geoPoint = GeoPoint(lat, lon)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("location", geoPoint)
            .addOnSuccessListener { Log.d("UserRepo", "Location updated") }
            .addOnFailureListener { e -> Log.e("UserRepo", "Failed to update location", e) }
    }

    suspend fun getOtherPatients(): List<User?> {
        val caretakerId = getCurrentUser()?.uid ?: return emptyList()

        val caretakerDoc = firebaseCollection.document(caretakerId).get().await()
        val patientIds = caretakerDoc.get("patients") as? List<String> ?: emptyList()

        val selectedPatientId = caretakerDoc.get("selectedPatient") as? String
            ?: return emptyList()

        val otherPatientIds = patientIds.filter { it != selectedPatientId }

        val otherPatients = otherPatientIds.map { patientId ->
            firebaseCollection.document(patientId).get().await().toObject(User::class.java)
        }

        return otherPatients
    }

    suspend fun selectPatient(patientId: String): User? {
        val userId = getCurrentUser()?.uid ?: return null

        return try {
            firebaseCollection.document(userId)
                .update("selectedPatient", patientId)
                .await()

            getUser(patientId)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error selecting patient: ${e.message}", e)
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

    suspend fun getUser(): User? {
        val currentUser = authentication.currentUser ?: return null
        val userSnapshot = firebaseCollection.document(currentUser.uid).get().await()
        return userSnapshot.toObject(User::class.java)
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

    suspend fun findPatientsByQuery(query: String): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firebaseCollection
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()

                val allPatients = snapshot.toObjects(User::class.java)
                val lowerQuery = query.trim().lowercase()

                allPatients.filter { user ->
                    (user.username?.trim()?.lowercase()?.contains(lowerQuery) == true ||
                            (user.firstName?.trim()?.lowercase()?.contains(lowerQuery) == true ||
                                    (user.lastName?.trim()?.lowercase()?.contains(lowerQuery) == true ||
                                            (user.phoneNumber?.trim()?.lowercase()?.contains(lowerQuery) == true) ||
                                            (user.email?.trim()?.lowercase()?.contains(lowerQuery) == true)
                                            )))
                }
            } catch (e: Exception) {
                emptyList()
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

//    fun updateUserLocation(userId: String, latitude: Double, longitude: Double) {
//        val db = FirebaseFirestore.getInstance()
//
//        val locationData = mapOf(
//            "location" to GeoPoint(latitude, longitude),
//            "timestamp" to System.currentTimeMillis()
//        )
//
//        db.collection("users").document(userId)
//            .update(locationData)
//            .addOnSuccessListener {
//                Log.d("LocationUpdate", "Locația a fost salvată.")
//            }
//            .addOnFailureListener { e ->
//                Log.e("LocationUpdate", "Eroare la salvarea locației", e)
//            }
//    }

    fun appendLocationToCurrentRoute(userId: String, latitude: Double, longitude: Double, arrivalTime: Long, duration: Long) {
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId)

        // Creează un nou punct cu toate datele
        val newPoint = mapOf(
            "location" to GeoPoint(latitude, longitude),
            "arrivalTime" to arrivalTime,
            "duration" to duration
        )

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            val currentRouteRaw = snapshot.get("currentRoute") as? List<Map<String, Any>>

            val updatedRoute = currentRouteRaw?.toMutableList() ?: mutableListOf()

            updatedRoute.add(newPoint)

            transaction.update(userDoc, "currentRoute", updatedRoute)
        }.addOnFailureListener { e ->
            Log.e("UserRepository", "Failed to update currentRoute: ${e.message}")
        }
    }

    fun updateDurationForLastLocation(userId: String, duration: Long) {
        val userDocRef = database.collection("users").document(userId)

        database.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val currentRouteRaw = snapshot.get("currentRoute") as? List<Map<String, Any>>

            if (currentRouteRaw != null && currentRouteRaw.isNotEmpty()) {
                // Convertim lista la MutableList pentru modificare
                val currentRoute = currentRouteRaw.toMutableList()

                // Ultimul punct — copiem map-ul ca mutable
                val lastPoint = currentRoute.last().toMutableMap()

                // Actualizăm durata
                lastPoint["duration"] = duration

                // Înlocuim ultimul punct cu cel modificat
                currentRoute[currentRoute.size - 1] = lastPoint

                // Scriem înapoi lista actualizată
                transaction.update(userDocRef, "currentRoute", currentRoute)
            }
        }.addOnSuccessListener {
            Log.d("UserRepository", "Duration updated successfully")
        }.addOnFailureListener { e ->
            Log.e("UserRepository", "Failed to update duration", e)
        }
    }

    suspend fun getFcmToken(userId: String): String? {
        return try {
            val userDoc = firebaseCollection.document(userId).get().await()
            userDoc.getString("fcmToken")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting FCM token: ${e.message}", e)
            null
        }
    }

    fun observePatientLocation(patientId: String): Flow<GeoPoint?> = callbackFlow {
        val docRef = FirebaseFirestore.getInstance().collection("users").document(patientId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            val currentRouteRaw = snapshot.get("currentRoute") as? List<*>
            val currentRoute = currentRouteRaw?.mapNotNull { point ->
                if (point is Map<*, *>) {
                    val lat = point["latitude"] as? Double
                    val lng = point["longitude"] as? Double
                    if (lat != null && lng != null) GeoPoint(lat, lng) else null
                } else null
            }

            val lastPoint = currentRoute?.lastOrNull()
            trySend(lastPoint)
        }

        awaitClose { registration.remove() }
    }

    fun resetCurrentRoute(userId: String) {
        firebaseCollection
            .document(userId)
            .collection("currentRoute")
            .get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots) {
                    document.reference.delete()
                }
            }
    }
}