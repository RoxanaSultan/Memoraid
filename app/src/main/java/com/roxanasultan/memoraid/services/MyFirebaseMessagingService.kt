package com.roxanasultan.memoraid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "medication_channel"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("MyFirebaseMessagingService", "Message received: ${remoteMessage.data}")

        val medId = remoteMessage.data["medId"]

        if (!medId.isNullOrEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("medicine").document(medId).get()
                .addOnSuccessListener { document ->
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val date: Date? = try {
                            formatter.parse(medicine.date)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        if (date != null) {
                            Log.d("FCM", "Scheduling alarm for medication: ${medicine.name} on date: ${medicine.date}")
                            AlarmScheduler.scheduleAlarmForMedication(this, medicine, date)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("FCM", "Failed to fetch medicine $medId", it)
                }
        }

        val title = remoteMessage.notification?.title ?: "New medication added!"
        val body = remoteMessage.notification?.body ?: "New medication has been added to your calendar."

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", remoteMessage.data["navigate_to"])
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.medicine)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}