package com.roxanasultan.memoraid.helpers

import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.roxanasultan.memoraid.models.Appointment
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.receivers.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AlarmScheduler {

    fun scheduleAlarmForMedication(context: Context, medication: Medicine, date: Date? = Calendar.getInstance().time) {
        Log.d("AlarmScheduler", "scheduleAlarmForMedication called with medication: ${medication.name}, date: $date")

        if (date == null) {
            Log.e("AlarmScheduler", "Date is null, cannot schedule alarm.")
            return
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(date)

        val timeList = medication.time.split(":")
        val hour = timeList.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeList.getOrNull(1)?.toIntOrNull() ?: 0

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicationId", medication.id)
            putExtra("name", medication.name)
            putExtra("dose", medication.dose)
            putExtra("time", medication.time)
            putExtra("date", dateAsString)
            putExtra("note", medication.note)
        }

        val requestCode = (medication.id + dateAsString).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val alarmPermissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(alarmPermissionIntent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarmForMedication(context: Context, medication: Medicine, date: Date) {
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(date)

        val requestCode = (medication.id + dateAsString).hashCode()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicationId", medication.id)
            putExtra("date", dateAsString)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Log.d("AlarmScheduler", "Cancelled alarm for medication ${medication.id} on date $dateAsString")
    }

    fun scheduleAlarmForAppointment(context: Context, appointment: Appointment, date: Date? = Calendar.getInstance().time) {
        Log.d("AlarmScheduler", "scheduleAlarmForAppointment called with appointment: ${appointment.name}, date: $date")

        if (date == null) {
            Log.e("AlarmScheduler", "Date is null, cannot schedule alarm.")
            return
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(date)

        val timeList = appointment.time.split(":")
        val hour = timeList.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeList.getOrNull(1)?.toIntOrNull() ?: 0

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("appointmentId", appointment.id)
            putExtra("name", appointment.name)
            putExtra("doctor", appointment.doctor)
            putExtra("time", appointment.time)
            putExtra("date", dateAsString)
            putExtra("location", appointment.location)
        }

        val requestCode = (appointment.id + dateAsString).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val alarmPermissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(alarmPermissionIntent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Log.d("AlarmScheduler", "Appointment details: Name=${
            appointment.name
        }, Doctor=${appointment.doctor}, Location=${appointment.location}, Time=${appointment.time}, Date=$dateAsString")
    }

    fun cancelAlarmForAppointment(context: Context, appointment: Appointment, date: Date) {
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(date)

        val requestCode = (appointment.id + dateAsString).hashCode()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("appointmentId", appointment.id)
            putExtra("date", dateAsString)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Log.d("AlarmScheduler", "Cancelled alarm for appointment ${appointment.id} on date $dateAsString")
    }
}