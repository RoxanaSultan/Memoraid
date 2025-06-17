package com.roxanasultan.memoraid.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val MAX_ACCEPTABLE_ACCURACY = 50f
        const val LOCATION_UPDATE_INTERVAL_MS = 5_000L
    }

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.Main).launch {
            val userType = userRepository.getUserRole()

            if (userType != "patient") {
                stopSelf()
                return@launch
            }

            createNotificationChannel()
            val notification = NotificationCompat.Builder(this@LocationForegroundService, "location_channel")
                .setContentTitle("Memoraid is tracking your location")
                .setContentText("Location updates are active")
                .setSmallIcon(R.drawable.location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(1, notification)

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@LocationForegroundService)

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(5_000L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    if (location.accuracy > MAX_ACCEPTABLE_ACCURACY) {
                        Log.w("LocationService", "Low accuracy: ${location.accuracy}, skipping update")
                        return
                    }

                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                    userRepository.updatePatientLocation(userId, location.latitude, location.longitude)
                }
            }

            startLocationUpdates()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Trebuie să ceri permisiunea de la UI
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}