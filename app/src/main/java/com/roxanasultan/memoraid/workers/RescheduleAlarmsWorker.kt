package com.roxanasultan.memoraid.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine
import kotlinx.coroutines.tasks.await

class RescheduleAlarmsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.failure()

        val selectedPatientId = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            .getString("selectedPatient") ?: return Result.failure()

        val snapshot = firestore.collection("medicine")
            .whereEqualTo("userId", selectedPatientId)
            .get()
            .await()

        val medications = snapshot.documents.mapNotNull { doc ->
            val medicine = doc.toObject(Medicine::class.java)?.copy(id = doc.id)
            medicine?.copy(taken = doc.getBoolean("taken") ?: false)
        }

        for (med in medications) {
            if (med.hasAlarm) {
                AlarmScheduler.scheduleAlarmForMedication(context, med)
            }
        }

        return Result.success()
    }
}