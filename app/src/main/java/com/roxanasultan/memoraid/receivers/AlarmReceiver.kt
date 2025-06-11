package com.roxanasultan.memoraid.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.activities.MedicineReminderActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Appointment
import com.roxanasultan.memoraid.models.Medicine
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medicationId")
        val appointmentId = intent.getStringExtra("appointmentId")

        val channelId = "memoraid_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "memoraid:reminderWakeLock"
        )
        wakeLock.acquire(5000L)

        val db = FirebaseFirestore.getInstance()

        if (!medicationId.isNullOrEmpty()) {
            db.collection("medicine").document(medicationId).get()
                .addOnSuccessListener { document ->
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val fullScreenIntent = Intent(context, MedicineReminderActivity::class.java).apply {
                            putExtra("medicationId", medicine.id)
                            putExtra("name", medicine.name)
                            putExtra("dose", medicine.dose)
                            putExtra("time", medicine.time)
                            putExtra("date", medicine.date)
                            putExtra("note", medicine.note)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }

                        val fullScreenPendingIntent = PendingIntent.getActivity(
                            context, 100, fullScreenIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val notification = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(R.drawable.medicine)
                            .setContentTitle("Medication Reminder")
                            .setContentText("Time to take ${medicine.name}")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setContentIntent(fullScreenPendingIntent)
                            .setFullScreenIntent(fullScreenPendingIntent, true)
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

                        val today = Calendar.getInstance().time
                        val nextDate = getNextDate(medicine, today)
                        nextDate?.let {
                            AlarmScheduler.scheduleAlarmForMedication(context, medicine, it)
                        }
                    }
                }
        } else if (!appointmentId.isNullOrEmpty()) {
            db.collection("appointments").document(appointmentId).get()
                .addOnSuccessListener { document ->
                    val appointment = document.toObject(Appointment::class.java)
                    if (appointment != null) {
                        val contentIntent = Intent(context, MainActivity::class.java)
                        val contentPendingIntent = PendingIntent.getActivity(
                            context, 101, contentIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val notification = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(R.drawable.appointment)
                            .setContentTitle("Appointment Reminder")
                            .setContentText("You have an appointment: ${appointment.name}")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(contentPendingIntent)
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

                        val today = Calendar.getInstance().time
                        val nextDate = getNextDate(appointment, today)
                        nextDate?.let {
                            AlarmScheduler.scheduleAlarmForAppointment(context, appointment, it)
                        }
                    }
                }
        }
    }

    fun getNextDate(medicine: Medicine, date: Date): Date? {
        var nextDate: Date? = null

        val calendar = Calendar.getInstance().apply {
            time = date
        }

        fun dayOfWeekFromString(day: String): Int {
            return when (day.lowercase(Locale.getDefault())) {
                "sunday" -> Calendar.SUNDAY
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                else -> Calendar.MONDAY // default
            }
        }

        when (medicine.frequency) {
            "Daily" -> {
                // Adaugă o zi
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                nextDate = calendar.time
            }
            "Weekly" -> {
                // Avem o listă de zile (ex: ["Monday", "Wednesday"])
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weeklyDays = medicine.weeklyDays ?: return date // fallback

                // Convertim zilele în numere Calendar.DAY_OF_WEEK
                val daysOfWeek = weeklyDays.map { dayOfWeekFromString(it) }

                // Căutăm prima zi din săptămână din daysOfWeek care este după azi
                val sortedDays = daysOfWeek.sorted()
                for (day in sortedDays) {
                    if (day > todayDayOfWeek) {
                        val daysToAdd = day - todayDayOfWeek
                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        return calendar.time
                    }
                }
                // Dacă nu am găsit niciuna după azi, alegem prima zi din lista săptămânii de săptămâna viitoare
                val firstDay = sortedDays.first()
                val daysToAdd = 7 - todayDayOfWeek + firstDay
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                nextDate = calendar.time
            }
            "Every X days" -> {
                val x = medicine.everyXDays ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, x)
                nextDate = calendar.time
            }
            "Monthly" -> {
                val monthlyDay = medicine.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                // Mutăm luna la următoarea lună
                calendar.add(Calendar.MONTH, 1)
                // Setăm ziua din lună la cea dorită
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceAtMost(maxDay))
                nextDate = calendar.time
            }
            else -> {
                return null
            }
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(nextDate)

        if (medicine.skippedDates != null && medicine.skippedDates.contains(dateAsString))
        {
            return getNextDate(medicine, nextDate)
        } else if (medicine.endDate != null && dateAsString >= medicine.endDate){
            return null
        }
        else {
            return nextDate
        }
    }

    fun getNextDate(appointment: Appointment, date: Date): Date? {
        var nextDate: Date? = null

        val calendar = Calendar.getInstance().apply {
            time = date
        }

        fun dayOfWeekFromString(day: String): Int {
            return when (day.lowercase(Locale.getDefault())) {
                "sunday" -> Calendar.SUNDAY
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                else -> Calendar.MONDAY // default
            }
        }

        when (appointment.frequency) {
            "Daily" -> {
                // Adaugă o zi
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                nextDate = calendar.time
            }
            "Weekly" -> {
                // Avem o listă de zile (ex: ["Monday", "Wednesday"])
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weeklyDays = appointment.weeklyDays ?: return date // fallback

                // Convertim zilele în numere Calendar.DAY_OF_WEEK
                val daysOfWeek = weeklyDays.map { dayOfWeekFromString(it) }

                // Căutăm prima zi din săptămână din daysOfWeek care este după azi
                val sortedDays = daysOfWeek.sorted()
                for (day in sortedDays) {
                    if (day > todayDayOfWeek) {
                        val daysToAdd = day - todayDayOfWeek
                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        return calendar.time
                    }
                }
                // Dacă nu am găsit niciuna după azi, alegem prima zi din lista săptămânii de săptămâna viitoare
                val firstDay = sortedDays.first()
                val daysToAdd = 7 - todayDayOfWeek + firstDay
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                nextDate = calendar.time
            }
            "Every X days" -> {
                val x = appointment.everyXDays ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, x)
                nextDate = calendar.time
            }
            "Monthly" -> {
                val monthlyDay = appointment.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                // Mutăm luna la următoarea lună
                calendar.add(Calendar.MONTH, 1)
                // Setăm ziua din lună la cea dorită
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceAtMost(maxDay))
                nextDate = calendar.time
            }
            else -> {
                return null
            }
        }

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateAsString = formatter.format(nextDate)

        if (appointment.skippedDates != null && appointment.skippedDates.contains(dateAsString))
        {
            return getNextDate(appointment, nextDate)
        } else if (appointment.endDate != null && dateAsString >= appointment.endDate){
            return null
        }
        else {
            return nextDate
        }
    }
}