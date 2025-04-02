package com.example.memoraid.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.memoraid.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appointmentName = intent.getStringExtra("appointment_name") ?: "Appointment"
        val appointmentTime = intent.getStringExtra("appointment_time") ?: ""
        val appointmentDate = intent.getStringExtra("appointment_date") ?: ""

        // Show notification
        showNotification(context, appointmentName, "$appointmentDate $appointmentTime")

        // Also show toast for debugging
        Toast.makeText(
            context,
            "Reminder: $appointmentName at $appointmentTime",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "appointment_channel",
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for appointment reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "appointment_channel")
            .setSmallIcon(R.drawable.notification) // Make sure you have this icon
            .setContentTitle(title)
            .setContentText("Reminder: $content")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(title.hashCode(), notification)
    }
}