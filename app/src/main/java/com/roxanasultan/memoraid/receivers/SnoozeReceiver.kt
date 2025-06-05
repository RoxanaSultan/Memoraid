package com.roxanasultan.memoraid.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roxanasultan.memoraid.receivers.AlarmReceiver
import java.util.Calendar

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val snoozeMinutes = 5
        val medicationId = intent.getStringExtra("medicationId") ?: return
        val name = intent.getStringExtra("name") ?: ""
        val dose = intent.getStringExtra("dose") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val note = intent.getStringExtra("note") ?: ""

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeMinutes)
        }

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicationId", medicationId)
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("time", time)
            putExtra("date", date)
            putExtra("note", note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode() + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d("SnoozeReceiver", "Snoozed alarm for $name in $snoozeMinutes min")
    }
}