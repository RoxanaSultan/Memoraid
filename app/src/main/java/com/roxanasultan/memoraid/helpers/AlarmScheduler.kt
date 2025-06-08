package com.roxanasultan.memoraid.helpers

import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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

        val medicationId = medication.id
        val name = medication.name
        val dateMedication = dateAsString
        val dose = medication.dose
        val note = medication.note
        val timeMedication = medication.time

        Log.d("AlarmScheduler", "Scheduling alarm for medication: $medicationId at $hour:$minute in $date")
        Log.d("AlarmScheduler", "Medication details: Name=$name, Dose=$dose, Time=$timeMedication, Date=$dateMedication, Note=$note")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicationId", medicationId)
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("time", timeMedication)
            putExtra("date", dateMedication)
            putExtra("note", note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
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
}