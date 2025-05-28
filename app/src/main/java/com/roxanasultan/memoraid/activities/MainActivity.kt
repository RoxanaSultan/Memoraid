package com.roxanasultan.memoraid.activities

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.roxanasultan.memoraid.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.roxanasultan.memoraid.helpers.AlarmScheduler
import com.roxanasultan.memoraid.patient.viewmodels.MedicationViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.roxanasultan.memoraid.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import androidx.work.*
import com.roxanasultan.memoraid.workers.RescheduleAlarmsWorker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val userViewModel: UserViewModel by viewModels()
    private val medicationViewModel: MedicationViewModel by viewModels()

    private val REQUEST_LOCATION_PERMISSION_CODE = 1001
    private val REQUEST_NOTIFICATION_PERMISSION_CODE = 1002

    private val CHANNEL_ID = "medication_channel"
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        requestNotificationPermission()

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        userViewModel.loadUser()
        userViewModel.fetchUserRole()

        userViewModel.userRole.observe(this) { role ->
            bottomNavigationView.menu.clear()
            val navInflater = navController.navInflater

            when (role) {
                "patient" -> {
                    bottomNavigationView.inflateMenu(R.menu.bottom_navigator_patient)
                    val graph = navInflater.inflate(R.navigation.navigation_graph_patient)
                    navController.graph = graph

                    requestLocationPermissions()

                    userViewModel.user.value?.id?.let { medicationViewModel.loadAllMedicationForUser(it) }
                    scheduleAlarms()

                    val periodicRequest = PeriodicWorkRequestBuilder<RescheduleAlarmsWorker>(
                        12, TimeUnit.HOURS
                    ).build()

                    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                        "rescheduleAlarms",
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                }

                "caretaker" -> {
                    bottomNavigationView.inflateMenu(R.menu.bottom_navigator_caretaker)
                    val graph = navInflater.inflate(R.navigation.navigation_graph_caretaker)
                    navController.graph = graph
                }

                else -> Log.e("MainActivity", "Unknown role")
            }

            bottomNavigationView.setupWithNavController(navController)

            if (savedInstanceState == null) {
                bottomNavigationView.selectedItemId = R.id.navigation_account
            }

            handleIntent(intent)

            val userId = userViewModel.user.value?.id
            if (userId != null) {
                FirebaseMessaging.getInstance().subscribeToTopic(userId)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseTopic", "Subscribed to topic: $userId")
                        } else {
                            Log.e("FirebaseTopic", "Failed to subscribe", task.exception)
                        }
                    }
            }
        }

        getFcmToken()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val navigateTo = intent.getStringExtra("navigate_to")
        Log.d("MainActivity", "handleIntent navigate_to = $navigateTo")

        when (navigateTo) {
            "medication" -> navController.navigate(R.id.navigation_health)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Medication Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication reminders"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestLocationPermissions() {
        val fineLocationPermission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result.trim()
            Log.d("MainActivity", "FCM Token: $token")
            if (token != null) {
                saveTokenToFirestore(token)
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MainActivity", "User ID is null, can't save token")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("MainActivity", "FCM token saved successfully")
            }
            .addOnFailureListener {
                userDocRef.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("MainActivity", "FCM token set successfully with merge")
                    }
                    .addOnFailureListener { ex ->
                        Log.e("MainActivity", "Failed to save FCM token", ex)
                    }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.e("MainActivity", "All permissions for the location have been granted!")
                } else {
                    Log.e("MainActivity", "Location permissions have not been granted!")
                }
            }

            REQUEST_NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Notification permission granted")
                } else {
                    Log.d("MainActivity", "Notification permission denied")
                }
            }
        }
    }

    private fun scheduleAlarms() {
        lifecycleScope.launch {
            medicationViewModel.allMedication.collect { medications ->
                for (medication in medications) {
                    if (!medication.hasAlarm) {
                        AlarmScheduler.scheduleAlarmForMedication(this@MainActivity, medication)
                        medicationViewModel.setAlarm(medication.id, true)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}