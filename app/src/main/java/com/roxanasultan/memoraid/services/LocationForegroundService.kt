package com.roxanasultan.memoraid.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.receivers.ActivityRecognitionReceiver
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var activityRecognitionPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        activityRecognitionClient = ActivityRecognition.getClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

                userRepository.updateUserLocation(userId, location.latitude, location.longitude)
            }
        }

        setupLocationRequest(intervalMillis = 10_000L)

        requestActivityUpdates()
    }

    private fun setupLocationRequest(intervalMillis: Long) {
        locationRequest = LocationRequest.create().apply {
            interval = intervalMillis
            fastestInterval = intervalMillis / 2
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        // Dacă updatezi intervalul, trebuie să reîncepi cererea location updates
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun requestActivityUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Activity Recognition permission not granted")
            return
        }

        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        activityRecognitionPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        activityRecognitionClient.requestActivityUpdates(
            5_000L, // verifică activitatea la fiecare 5 secunde
            activityRecognitionPendingIntent
        )
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

        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}