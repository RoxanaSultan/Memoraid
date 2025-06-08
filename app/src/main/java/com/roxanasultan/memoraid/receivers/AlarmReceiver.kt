package com.roxanasultan.memoraid.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.activities.MedicineReminderActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: ""
        val dose = intent.getStringExtra("dose") ?: "0"
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val note = intent.getStringExtra("note") ?: ""
        val medicationId = intent.getStringExtra("medicationId") ?: ""

        Log.d("AlarmReceiver", "Alarm received for medication $name")

        val fullScreenIntent = Intent(context, MedicineReminderActivity::class.java).apply {
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("note", note)
            putExtra("medicationId", medicationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "medication_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for medication reminders"
            notificationManager.createNotificationChannel(channel)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "memoraid:medicationReminderWakeLock"
        )
        wakeLock.acquire(5000L)

        context.startActivity(fullScreenIntent)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.medicine)
            .setContentTitle("Medication to take!")
            .setContentText("It's time to take your medication: $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        val db = FirebaseFirestore.getInstance()
        db.collection("medicine").document(medicationId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val nextDate = getNextDate(medicine)
                        Log.d("AlarmReceiver", "Next date for medication $name: $nextDate")
                        AlarmScheduler.scheduleAlarmForMedication(context, medicine, nextDate)
                    }
                }
            }
    }


    fun getNextDate(medicine: Medicine): Date? {
        val calendar = Calendar.getInstance()
        val today = calendar.time

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
                return calendar.time
            }
            "Weekly" -> {
                // Avem o listă de zile (ex: ["Monday", "Wednesday"])
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val weeklyDays = medicine.weeklyDays ?: return today // fallback

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
                return calendar.time
            }
            "Every X days" -> {
                val x = medicine.everyXDays ?: 1
                calendar.add(Calendar.DAY_OF_YEAR, x)
                return calendar.time
            }
            "Monthly" -> {
                val monthlyDay = medicine.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                // Mutăm luna la următoarea lună
                calendar.add(Calendar.MONTH, 1)
                // Setăm ziua din lună la cea dorită
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, monthlyDay.coerceAtMost(maxDay))
                return calendar.time
            }
            else -> {
                return null
            }
        }
    }
}