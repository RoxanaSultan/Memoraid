package com.roxanasultan.memoraid.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.random.Random
import com.roxanasultan.memoraid.R

class ForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "memoraid_channel")
            .setContentTitle("Memoraid rulează")
            .setContentText("Urmărim locația și trimitem memento-uri.")
            .setSmallIcon(R.drawable.notification)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        startLocationUpdates()
        startReminderLoop()

        return START_STICKY
    }

    private fun startReminderLoop() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(60_000) // verifică la fiecare minut

                // Ex: trimite o notificare
                sendReminder("Este timpul pentru o pastilă!")
            }
        }
    }

    private fun sendReminder(message: String) {
        val notification = NotificationCompat.Builder(this, "memoraid_channel")
            .setContentTitle("Memoraid")
            .setContentText(message)
            .setSmallIcon(R.drawable.notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(), notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5 * 60 * 1000 // la fiecare 5 minute
            fastestInterval = 2 * 60 * 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation
                        Log.d("Location", "Lat: ${location?.latitude}, Lon: ${location?.longitude}")
                        // Poți salva în Firestore, local DB, etc.
                    }
                },
                Looper.getMainLooper()
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "memoraid_channel",
                "Memoraid Foreground Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}