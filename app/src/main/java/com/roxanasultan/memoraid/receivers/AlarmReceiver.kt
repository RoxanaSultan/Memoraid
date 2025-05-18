package com.roxanasultan.memoraid.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MedicineReminderActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val patientId = intent.getStringExtra("USER_ID") ?: "Unknown"

        val activityIntent = Intent(context, MedicineReminderActivity::class.java).apply {
            putExtra("USER_ID", patientId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "medicine_reminder_channel")
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("Medicine Reminder")
            .setContentText("It's time for patient $patientId to take their medicine!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(patientId.hashCode(), builder.build())
        }
    }
}