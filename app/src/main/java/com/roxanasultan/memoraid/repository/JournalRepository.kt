package com.roxanasultan.memoraid.repository

import android.net.Uri
import com.roxanasultan.memoraid.models.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val firestoreCollection get() = firestore.collection("journals")
    private val storageReference  get() = storage.reference.child("journal_images")

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw SecurityException("User not authenticated")
    }

    suspend fun loadJournals(): List<Journal> {
        val userId = requireUserId()
        return firestoreCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Journal::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun getJournal(journalId: String): Journal? {
        val userId = requireUserId()
        return firestoreCollection
            .document(journalId)
            .get()
            .await()
            .takeIf { doc ->
                doc.exists() && doc.getString("userId") == userId
            }
            ?.toObject(Journal::class.java)
            ?.copy(id = journalId)
    }

    suspend fun createJournal(journalType: String): String {
        val userId = requireUserId()
        val newJournal = Journal(
            userId = userId,
            entryDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            title = "Untitled",
            text = "",
            type = journalType
        )

        val docRef = firestoreCollection.document()
        docRef.set(newJournal).await()
        return docRef.id
    }

    suspend fun saveJournal(journal: Journal) {
        requireUserId()
        firestoreCollection.document(journal.id)
            .set(journal)
            .await()
    }

    suspend fun deleteJournal(journalId: String): Boolean {
        val document = firestoreCollection.document(journalId).get().await()
        val imageUris = document.get("imageUris") as? List<String> ?: emptyList()

        for (uri in imageUris) {
            try {
                storage.getReferenceFromUrl(uri).delete().await()
            } catch (_: Exception) {
            }
        }

        firestoreCollection.document(journalId).delete().await()
        return true
    }

    suspend fun loadJournalDetails(journalId: String): Journal? {
        return try {
            val document = firestoreCollection.document(journalId).get().await()
            document.toObject(Journal::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveJournalDetails(journal: Journal): Boolean {
        return try {
            firestoreCollection.document(journal.id!!).set(journal).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeImageFromFirestore(imageUri: String): Boolean {
        return try {
            firestoreCollection.whereArrayContains("imageUris", imageUri).get().await().documents.forEach { document ->
                val updatedUris = (document["imageUris"] as List<String>).filter { it != imageUri }
                firestoreCollection.document(document.id).update("imageUris", updatedUris).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeImageFromStorage(imageUri: String): Boolean {
        return try {
            val fileReference = storage.getReferenceFromUrl(imageUri)
            fileReference.delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadImageToStorage(uri: Uri): String? {
        return try {
            val fileName = "${System.currentTimeMillis()}.jpg"
            val fileReference = storageReference.child(fileName)
            fileReference.putFile(uri).await()
            fileReference.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkIfImageExistsInStorage(imageUri: String): Boolean {
        return try {
            val fileReference = storage.getReferenceFromUrl(imageUri)
            fileReference.downloadUrl.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}