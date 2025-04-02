package com.example.memoraid.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.memoraid.models.Appointment
import com.example.memoraid.receivers.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {
    private const val TAG = "AlarmHelper"

    fun setAlarm(context: Context, appointment: Appointment) {
        val alarmTime = appointment.getAlarmTimeInMillis()

        if (alarmTime <= 0) {
            Log.e(TAG, "Invalid alarm time for appointment: ${appointment.name}")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Only set alarm if it's in the future (with 5 second buffer)
        if (alarmTime > currentTime + 5000) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("appointment_id", appointment.id)
                putExtra("appointment_name", appointment.name)
                putExtra("appointment_time", appointment.time)
                putExtra("appointment_date", appointment.date)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.id.hashCode(),
                intent,
                flags
            )

            // Use setExactAndAllowWhileIdle for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Alarm set for ${appointment.name} at ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(alarmTime))}")
        } else {
            Log.d(TAG, "Not setting alarm for ${appointment.name} - time is in the past")
        }
    }

    fun cancelAlarm(context: Context, appointment: Appointment) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm canceled for ${appointment.name}")
    }
}