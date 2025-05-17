package com.example.memoraid.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.memoraid.models.Appointment
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {
    fun scheduleReminder(context: Context, appointment: Appointment) {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        val dateTime = LocalDateTime.parse("${appointment.date} ${appointment.time}", formatter)

        val oneDayBefore = dateTime.minusDays(1)
        scheduleExactAlarm(context, oneDayBefore, appointment, "Reminder: Your appointment is tomorrow!")

        val oneHourBefore = dateTime.minusHours(1)
        scheduleExactAlarm(context, oneHourBefore, appointment, "Reminder: Your appointment is in 1 hour!")

        scheduleExactAlarm(context, dateTime, appointment, "Reminder: Your appointment is now.")
    }

    private fun scheduleExactAlarm(
        context: Context,
        dateTime: LocalDateTime,
        appointment: Appointment,
        message: String
    ) {
        val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra("title", appointment.name)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (appointment.id + message).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            millis,
            pendingIntent
        )
    }
}