package com.roxanasultan.memoraid.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.activities.MedicineReminderActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: ""
        val dose = intent.getStringExtra("dose") ?: "0"
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val note = intent.getStringExtra("note") ?: ""
        val medicationId = intent.getStringExtra("medicationId") ?: ""

        Log.d("AlarmReceiver", "Alarm received for medication $name")

        val fullScreenIntent = Intent(context, MedicineReminderActivity::class.java).apply {
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("note", note)
            putExtra("medicationId", medicationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "medication_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for medication reminders"
            notificationManager.createNotificationChannel(channel)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "memoraid:medicationReminderWakeLock"
        )
        wakeLock.acquire(5000L)

        context.startActivity(fullScreenIntent)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.medicine)
            .setContentTitle("Medication to take!")
            .setContentText("It's time to take your medication: $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        val db = FirebaseFirestore.getInstance()
        db.collection("medicine").document(medicationId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val now = java.util.Calendar.getInstance().time
                        AlarmScheduler.scheduleAlarmForMedication(context, medicine, fromDate = now)
                    }
                }
            }
    }
}