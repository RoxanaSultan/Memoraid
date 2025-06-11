package com.roxanasultan.memoraid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.models.Medicine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "medication_channel"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("MyFirebaseMessagingService", "Message received: ${remoteMessage.data}")

        val medId = remoteMessage.data["medId"]
        val isDeleted = remoteMessage.data["deleted"] == "true"
        val isUpdated = remoteMessage.data["updated"] == "true"
        val isAdded = remoteMessage.data["added"] == "true"

        Log.d("FCM", "medId: $medId, isDeleted: $isDeleted, isUpdated: $isUpdated, isAdded: $isAdded")

        if (!medId.isNullOrEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("medicine").document(medId).get()
                .addOnSuccessListener { document ->
                    val medicine = document.toObject(Medicine::class.java)
                    if (medicine != null) {
                        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val date: Date? = try {
                            formatter.parse(medicine.nextAlarm)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        if (date != null) {
                            if (isDeleted) {
                                Log.d("FCM", "Cancelling alarm for deleted medication: ${medicine.name}")
                                AlarmScheduler.cancelAlarmForMedication(this, medicine, date)
                            } else if (isUpdated) {
                                Log.d("FCM", "Cancelling alarm for deleted medication: ${medicine.name}")
                                AlarmScheduler.cancelAlarmForMedication(this, medicine, date)
                                Log.d("FCM", "Scheduling updated alarm for medication: ${medicine.name}")
                                val nextDate = getNextDate(medicine, date)
                                AlarmScheduler.scheduleAlarmForMedication(this, medicine, nextDate)
                            } else if (isAdded) {
                                Log.d("FCM", "Scheduling updated alarm for medication: ${medicine.name}")
                                AlarmScheduler.scheduleAlarmForMedication(this, medicine, date)
                            }
                        }
                        else {
                            Log.e("FCM", "date is null")
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("FCM", "Failed to fetch medicine $medId", it)
                }
        }

        val title = remoteMessage.data["title"] ?: "Medication Alert"
        val body = remoteMessage.data["body"] ?: "Check your medication list."

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", remoteMessage.data["navigate_to"])
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.medicine)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
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
}