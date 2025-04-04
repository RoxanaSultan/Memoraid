package com.example.memoraid.repository

import android.widget.Toast
import com.example.memoraid.model.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class JournalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    suspend fun loadJournals(): List<Journal> {
        val currentUser = auth.currentUser?.uid ?: return emptyList()
        val snapshot = firestore.collection("journals")
            .whereEqualTo("userId", currentUser)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            it.toObject(Journal::class.java)?.copy(id = it.id)
        }
    }

    suspend fun deleteJournal(journalId: String): Boolean {
        val document = firestore.collection("journals").document(journalId).get().await()
        if (!document.exists()) return false

        val imageUris = document.get("imageUris") as? List<String> ?: emptyList()
        for (uri in imageUris) {
            try {
                storage.getReferenceFromUrl(uri).delete().await()
            } catch (_: Exception) {
            }
        }

        firestore.collection("journals").document(journalId).delete().await()
        return true
    }

    suspend fun createJournal(journalType: String): String? {
        val currentUser = auth.currentUser?.uid ?: return null

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date())

        val journalRef = firestore.collection("journals").document()
        val journalInfo = hashMapOf(
            "userId" to currentUser,
            "entryDate" to formattedDate,
            "title" to "Untitled",
            "text" to "",
            "imageUris" to listOf<String>(),
            "type" to journalType
        )

        journalRef.set(journalInfo).await()
        return journalRef.id
    }
}