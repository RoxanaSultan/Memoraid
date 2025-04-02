package com.example.memoraid.utils

import android.annotation.SuppressLint
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

object AppointmentAlarmManager {

    // In AppointmentAlarmManager.kt
    @SuppressLint("ScheduleExactAlarm")
    fun setAlarmForAppointment(context: Context, appointment: Appointment) {
        val appointmentDateTime = "${appointment.date} ${appointment.time}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentCalendar = Calendar.getInstance()

        Log.d("DateTimeDebug", """
    Raw date from appointment: ${appointment.date}
    Raw time from appointment: ${appointment.time}
    Combined string: $appointmentDateTime
    Current time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}
""".trimIndent())

        try {
            appointmentCalendar.time = dateFormat.parse(appointmentDateTime)!!
            Log.d("AlarmDebug", "Setting alarm for: ${appointment.name} at $appointmentDateTime")
            Log.d("AlarmDebug", "Parsed time: ${appointmentCalendar.time}")
        } catch (e: Exception) {
            Log.e("AlarmDebug", "Error parsing date: ${e.message}")
            e.printStackTrace()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (appointmentCalendar.timeInMillis > currentTime) {
            Log.d("AlarmDebug", "Alarm time is in the future, proceeding...")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("appointment_id", appointment.id)
                putExtra("appointment_name", appointment.name)
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

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                appointmentCalendar.timeInMillis,
                pendingIntent
            )

            Log.d("AlarmDebug", "Alarm set successfully for ${appointment.name}")
        } else {
            Log.d("AlarmDebug", "Alarm time is in the past, not setting")
        }
    }

    fun cancelAlarmForAppointment(context: Context, appointment: Appointment) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }
}