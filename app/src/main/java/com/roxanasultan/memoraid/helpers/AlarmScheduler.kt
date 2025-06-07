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

    fun getNextAlarmDate(medicine: Medicine, fromDate: Date): Date? {
        Log.d("AlarmScheduler", "getNextAlarmDate called for medicine: ${medicine.name} with frequency: ${medicine.frequency}")

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = fromDate

        return when (medicine.frequency.uppercase()) {
            "ONCE" -> {
                val medDateOnly = sdf.parse(medicine.date) ?: return null

                val medDateTime = Calendar.getInstance().apply {
                    time = medDateOnly
                    val timeParts = medicine.time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                    set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                return if (medDateTime.after(fromDate)) medDateTime else null
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

                if (originalTime.before(medTime)) {
                    return medTime.time
                }

                medTime.add(Calendar.DAY_OF_MONTH, 1)
                return medTime.time
            }

            "EVERY X DAYS" -> {
                val startDate = sdf.parse(medicine.date) ?: return null
                val x = medicine.everyXDays ?: return null

                val timeParts = medicine.time.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                val calendarNow = Calendar.getInstance().apply { time = fromDate }

                // Setăm ora din zi pentru comparație exactă
                val calendarNowWithTime = Calendar.getInstance().apply {
                    time = fromDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val startCalendar = Calendar.getInstance().apply {
                    time = startDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val diffMillis = calendarNow.timeInMillis - startCalendar.timeInMillis
                val daysDiff = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                return if (daysDiff < 0) {
                    // Azi e înainte de start, deci returnează startDate
                    startCalendar.time
                } else {
                    // E azi zi validă? (verificăm modulo)
                    val isTodayValid = daysDiff % x == 0
                    if (isTodayValid && calendarNow.before(calendarNowWithTime)) {
                        // Azi e o zi validă și ora n-a trecut
                        calendarNowWithTime.time
                    } else {
                        // Caută următoarea zi validă (adaugă x - (daysDiff % x))
                        val daysUntilNext = if (isTodayValid) x else x - (daysDiff % x)
                        val nextValid = Calendar.getInstance().apply {
                            time = calendarNow.time
                            add(Calendar.DAY_OF_MONTH, daysUntilNext)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        nextValid.time
                    }
                }
            }

            "WEEKLY" -> {
                val days = medicine.weeklyDays?.map { it.uppercase() } ?: return null
                for (i in 0..13) { // caută următoarea zi din săptămâna următoare (2 săptămâni max)
                    val day = Calendar.getInstance().apply {
                        time = fromDate
                        add(Calendar.DAY_OF_MONTH, i)
                        val timeParts = medicine.time.split(":")
                        set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                        set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayOfWeek = day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.uppercase()
                    if (days.contains(dayOfWeek) && day.time.after(fromDate)) {
                        return day.time
                    }
                }
                null
            }

            "MONTHLY" -> {
                val day = medicine.monthlyDay ?: return null
                val next = Calendar.getInstance().apply {
                    time = fromDate
                    set(Calendar.DAY_OF_MONTH, day)
                    val timeParts = medicine.time.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                    set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (!next.time.after(fromDate)) {
                    next.add(Calendar.MONTH, 1)
                    next.set(Calendar.DAY_OF_MONTH, day)
                }

                return next.time
            }

            else -> null
        }
    }
}