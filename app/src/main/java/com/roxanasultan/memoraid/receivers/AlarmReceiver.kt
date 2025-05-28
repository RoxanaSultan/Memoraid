package com.roxanasultan.memoraid.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MedicineReminderActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")

        val dose = intent.getStringExtra("dose") ?: "medicamentul"

        val fullScreenIntent = Intent(context, MedicineReminderActivity::class.java).apply {
            putExtra("dose", dose)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

        // Construim notificarea cu FULL SCREEN INTENT
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.medicine)
            .setContentTitle("E timpul pentru pastile")
            .setContentText("Trebuie să iei: $dose")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)  // cheia aici!
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        Log.d("AlarmReceiver", "Notification with full screen intent sent for dose: $dose")
    }
}