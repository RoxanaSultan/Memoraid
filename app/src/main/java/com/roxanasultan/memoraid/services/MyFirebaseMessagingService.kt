package com.roxanasultan.memoraid.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MedicineReminderActivity
import java.util.Calendar

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Această metodă este apelată atunci când FCM generează un nou token sau reînnoiește tokenul existent.
     * Salvăm tokenul în Firestore pentru a te asigura că Cloud Functions trimite notificările către tokenul actual.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMessagingService", "New token: $token")
        saveTokenToFirestore(token)
    }

    /**
     * Metoda care actualizează tokenul în Firestore. Verificăm ca utilizatorul să fie autentificat,
     * apoi actualizăm documentul asociat în colecția "users" cu noul token.
     */
    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MyFirebaseMessagingService", "User is not authenticated, token not saved")
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)
        userDocRef.set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener { Log.d("MyFirebaseMessagingService", "Token updated successfully") }
            .addOnFailureListener { e -> Log.e("MyFirebaseMessagingService", "Failed to update token", e) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("MyFirebaseMessagingService", "Message received: ${remoteMessage.data}")

        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            Log.d("MyFirebaseMessagingService", "Message type: $type")
            if (type == "medicationReminder") {
                val date = remoteMessage.data["date"] ?: ""
                val time = remoteMessage.data["time"] ?: ""
                Log.d("MyFirebaseMessagingService", "Medication reminder received - Date: $date, Time: $time")
                scheduleLocalMedicationReminder(date, time, remoteMessage.data)
            } else {
                Log.d("MyFirebaseMessagingService", "Other message type received")
                showNotification(remoteMessage.data["title"] ?: "Memoraid", remoteMessage.data["body"] ?: "")
            }
        } else if (remoteMessage.notification != null) {
            Log.d("MyFirebaseMessagingService", "FCM notification received: ${remoteMessage.notification?.title}")
            showNotification(remoteMessage.notification?.title ?: "Memoraid", remoteMessage.notification?.body ?: "")
        }
    }

    /**
     * Metoda de programare a alarmei locale pentru reamintirea medicației.
     * Parsează data și ora, configurează Calendar-ul și folosește AlarmManager pentru a seta PendingIntent-ul.
     */
    private fun scheduleLocalMedicationReminder(date: String, time: String, data: Map<String, String>) {
        Log.d("MyFirebaseMessagingService", "Scheduling reminder for - Date: $date, Time: $time")

        if (date.isEmpty() || time.isEmpty()) {
            Log.e("MyFirebaseMessagingService", "Error: date or time is missing!")
            showNotification("Medication Reminder", "Please check your medication details!")
            return
        }

        try {
            val dateParts = date.split("-")
            val timeParts = time.split(":")
            if (dateParts.size != 3 || timeParts.size != 2) {
                throw IllegalArgumentException("Invalid date/time format")
            }

            val day = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val year = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                Log.d("MyFirebaseMessagingService", "Time is in the past, rescheduling for tomorrow")
                calendar.add(Calendar.DATE, 1)
            }

            Log.d("MyFirebaseMessagingService", "Alarm scheduled for: ${calendar.time}")

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, MedicineReminderActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Log.d("MyFirebaseMessagingService", "Alarm set with setExactAndAllowWhileIdle")
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Log.d("MyFirebaseMessagingService", "Alarm set with setExact")
            }
        } catch (e: Exception) {
            Log.e("MyFirebaseMessagingService", "Error scheduling alarm: ${e.message}")
            showNotification("Medication Reminder", "Error scheduling alarm. Check your medication details!")
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "medicine_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.medicine)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}