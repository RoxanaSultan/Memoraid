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
    fun scheduleAlarmForMedication(context: Context, medication: Medicine, fromDate: Date = Calendar.getInstance().time) {
        val timeList = medication.time.split(":")
        val hour = timeList.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeList.getOrNull(1)?.toIntOrNull() ?: 0

        val medicationId = medication.id
        val name = medication.name
        val date = medication.date
        val dose = medication.dose
        val note = medication.note
        val timeMedication = medication.time

        val nextDate = getNextAlarmDate(medication, fromDate) ?: return

        Log.d("AlarmScheduler", "Scheduling alarm for medication: $medicationId at $hour:$minute")
        Log.d("AlarmScheduler", "Medication details: Name=$name, Dose=$dose, Time=$timeMedication, Date=$date, Note=$note")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicationId", medicationId)
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("time", timeMedication)
            putExtra("date", date)
            putExtra("note", note)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            time = nextDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        Log.d("AlarmScheduler", "Next alarm date: ${calendar.time}")

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

    fun getNextAlarmDate(medicine: Medicine, fromDate: Date): Date? {
        Log.d("AlarmScheduler", "getNextAlarmDate called for medicine: ${medicine.name} with frequency: ${medicine.frequency}")

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = fromDate

        return when (medicine.frequency.uppercase()) {
            "ONCE" -> {
                val medDate = sdf.parse(medicine.date)
                if (medDate != null && medDate.after(fromDate)) medDate else null
            }

            "DAILY" -> {
                val originalTime = Calendar.getInstance().apply {
                    time = fromDate
                }

                val medTime = Calendar.getInstance().apply {
                    time = fromDate
                    set(Calendar.HOUR_OF_DAY, medicine.time.split(":")[0].toInt())
                    set(Calendar.MINUTE, medicine.time.split(":")[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Dacă e înainte de ora programată, întoarce azi
                if (originalTime.before(medTime)) {
                    return medTime.time
                }

                // Dacă a trecut ora, întoarce mâine
                medTime.add(Calendar.DAY_OF_MONTH, 1)
                return medTime.time
            }

            "EVERY X DAYS" -> {
                val x = medicine.everyXDays ?: return null
                calendar.add(Calendar.DAY_OF_MONTH, x)
                calendar.time
            }

            "WEEKLY" -> {
                val days = medicine.weeklyDays?.map { it.uppercase() } ?: return null
                for (i in 1..7) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.uppercase()
                    if (days.contains(dayOfWeek)) return calendar.time
                }
                null
            }

            "MONTHLY" -> {
                val day = medicine.monthlyDay ?: return null
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.time
            }

            else -> null
        }
    }
}