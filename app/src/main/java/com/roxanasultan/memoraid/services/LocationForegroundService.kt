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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.receivers.ActivityRecognitionReceiver
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
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

    // Ultima locație salvată și momentul sosirii la acel punct
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var arrivalTime: Long = 0L

    // Buffer pentru a filtra fluctuațiile (jitter) ale GPS-ului
    private val locationBuffer = mutableListOf<android.location.Location>()
    private val BUFFER_SIZE = 3

    // Constante pentru praguri și intervale
    companion object {
        const val LOCATION_THRESHOLD_METERS = 10f       // Dacă distanța medie depășește 10 m, considerăm că s-a mișcat
        const val MAX_ACCEPTABLE_ACCURACY = 50f           // Acuratețea maxim acceptată (în metri)
        const val MIN_DURATION_UPDATE_MS = 10_000L        // Durata minimă pentru actualizarea punctului staționar (10 secunde)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        activityRecognitionClient = ActivityRecognition.getClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Verificăm acuratețea: dacă e prea mare (adică, prea puțin precisă), ignorăm actualizarea
                if (location.accuracy > MAX_ACCEPTABLE_ACCURACY) {
                    Log.w("LocationService", "Acuratețea (${location.accuracy}) este prea scăzută, se trece peste această actualizare")
                    return
                }

                // Adăugăm mostrele în buffer pentru a face un filtru prin mediere
                locationBuffer.add(location)
                if (locationBuffer.size < BUFFER_SIZE) {
                    // Dacă nu avem suficiente mostre, așteptăm următoarele actualizări
                    return
                }

                // Calculăm media coordonatelor din buffer
                val avgLat = locationBuffer.map { it.latitude }.average()
                val avgLon = locationBuffer.map { it.longitude }.average()
                locationBuffer.clear() // Resetăm bufferul pentru noi mostre

                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                val currentTime = System.currentTimeMillis()

                // Verificăm dacă trebuie să resetăm traseul (dacă a început o nouă zi)
                if (shouldResetRoute(currentTime)) {
                    userRepository.resetCurrentRoute(userId)
                    // Resetăm variabilele locale ce țin evidența traseului
                    lastLatitude = null
                    lastLongitude = null
                    arrivalTime = 0L
                }

                // Dacă nu avem încă un punct salvat (de exemplu, la început sau după reset), adăugăm primul punct:
                if (lastLatitude == null || lastLongitude == null) {
                    lastLatitude = avgLat
                    lastLongitude = avgLon
                    arrivalTime = currentTime
                    userRepository.appendLocationToCurrentRoute(
                        userId,
                        avgLat,
                        avgLon,
                        arrivalTime,
                        0L
                    )
                } else {
                    // Calculăm distanța dintre ultima locație salvată și poziția medie calculată
                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(
                        lastLatitude!!, lastLongitude!!,
                        avgLat, avgLon,
                        distance
                    )

                    if (distance[0] < LOCATION_THRESHOLD_METERS) {
                        // Dacă nu s-a mișcat semnificativ, actualizăm doar durata petrecută la acest punct
                        val duration = currentTime - arrivalTime
                        if (duration >= MIN_DURATION_UPDATE_MS) {
                            userRepository.updateDurationForLastLocation(userId, duration)
                        }
                    } else {
                        // Dacă s-a schimbat poziția peste prag, începem un nou punct în traseu
                        lastLatitude = avgLat
                        lastLongitude = avgLon
                        arrivalTime = currentTime
                        userRepository.appendLocationToCurrentRoute(
                            userId,
                            avgLat,
                            avgLon,
                            arrivalTime,
                            0L
                        )
                    }
                }
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
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Permisiunea pentru locație nu este acordată")
            stopSelf()
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun requestActivityUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Permisiunea pentru recunoașterea activității nu este acordată")
            return
        }
        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        activityRecognitionPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        activityRecognitionClient.requestActivityUpdates(5_000L, activityRecognitionPendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("emergency_channel", "Emergency Location Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("LocationService", "Utilizatorul nu este autentificat.")
            stopSelf()
            return START_NOT_STICKY
        }
        val notification: Notification = NotificationCompat.Builder(this, "emergency_channel")
            .setContentTitle("Trimitere locație activă")
            .setContentText("Locația ta este transmisă periodic.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, notification)
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Funcție helper pentru a formata timestamp-ul într-o dată în format "yyyy-MM-dd"
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Verifică dacă trebuie resetat traseul: dacă data curentă diferă de data la care a început traseul (arrivalTime)
    private fun shouldResetRoute(currentTime: Long): Boolean {
        Log.d("LocationService", "Verificare resetare traseu: currentTime=$currentTime, arrivalTime=$arrivalTime")
        val currentDate = formatDate(currentTime)
        val routeDate = formatDate(arrivalTime)
        return currentDate != routeDate
    }
}