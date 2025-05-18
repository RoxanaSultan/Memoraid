package com.roxanasultan.memoraid.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.repository.UserRepository
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10_000
            fastestInterval = 5_000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

                userRepository.updateUserLocation(userId, location.latitude, location.longitude)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "emergency_channel",
                "Emergency Location Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Log.e("LocationService", "The user is not authenticated.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification: Notification = NotificationCompat.Builder(this, "emergency_channel")
            .setContentTitle("Send location active.")
            .setContentText("Your location is sent periodically.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}