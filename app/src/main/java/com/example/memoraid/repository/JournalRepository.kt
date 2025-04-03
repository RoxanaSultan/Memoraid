package com.example.memoraid.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

class JournalRepository @Inject constructor(
    private val database: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    fun getJournals(userId: String): Task<QuerySnapshot> {
        return database.collection("journals")
            .whereEqualTo("userId", userId)
            .get()
    }

    fun createJournal(journalInfo: Map<String, Any>): Task<Void> {
        val journalRef = database.collection("journals").document()
        return journalRef.set(journalInfo)
    }

    // Add other methods like deleteJournal, deleteImagesFromStorage etc.
}
