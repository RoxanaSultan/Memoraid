package com.roxanasultan.memoraid.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.helpers.NotificationHelper

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMessagingService", "New token: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MyFirebaseMessagingService", "User is not authenticated, token not saved")
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)
        userDocRef.set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("MyFirebaseMessagingService", "Token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("MyFirebaseMessagingService", "Failed to update token", e)
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("MyFirebaseMessagingService", "From: ${remoteMessage.from}")
        val data = remoteMessage.data
        val type = data["type"]

        when (type) {
            "newMedication" -> {
                // Notificare pentru medicament nou creat
                val medicationName = data["medicationName"] ?: "Medicament nou"
                val time = data["time"] ?: ""
                NotificationHelper.showNewMedicationNotification(
                    applicationContext,
                    "Medicament nou adăugat",
                    "$medicationName - programat la $time"
                )

                // Programează alarmele pentru acest medicament
                val hour = data["hour"]?.toIntOrNull() ?: return
                val minute = data["minute"]?.toIntOrNull() ?: return
                val dose = data["dose"] ?: "medicamentul"

                AlarmScheduler.scheduleAlarm(applicationContext, hour, minute, dose)
            }
        }
    }
}