package com.roxanasultan.memoraid.helpers

import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.roxanasultan.memoraid.receivers.AlarmReceiver

object AlarmScheduler {
    fun scheduleAlarm(context: Context, hour: Int, minute: Int, dose: String) {
        Log.d("AlarmScheduler", "Scheduling alarm: hour=$hour, minute=$minute, dose=$dose")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("dose", dose)
        }

        // Folosește un ID unic pentru fiecare alarm
        val alarmId = (hour * 100 + minute) // Ex: 14:30 -> 1430
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Dacă ora a trecut pentru azi, programează pentru mâine
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        // Programează alarm repetat zilnic
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // Repetă zilnic
            pendingIntent
        )

        Log.d("AlarmScheduler", "Alarm scheduled for: ${calendar.time}")

        // Salvare în SharedPreferences
        val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("alarm_${alarmId}_hour", hour)
            .putInt("alarm_${alarmId}_minute", minute)
            .putString("alarm_${alarmId}_dose", dose)
            .apply()
    }
}