package com.example.memoraid.activities

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.memoraid.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import com.example.memoraid.viewmodel.UserViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val userViewModel: UserViewModel by viewModels()

    private val REQUEST_LOCATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        userViewModel.userRole.observe(this) { role ->
            bottomNavigationView.menu.clear()

            val navInflater = navController.navInflater

            when (role) {
                "patient" -> {
                    bottomNavigationView.inflateMenu(R.menu.bottom_navigator_patient)
                    val graph = navInflater.inflate(R.navigation.navigation_graph_patient)
                    navController.graph = graph
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
        }

        userViewModel.fetchUserRole()

        requestLocationPermissions()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "location_service_channel"
            val channelName = "Location Service"
            val channelDescription = "Notifications for location foreground service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestLocationPermissions() {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.e("MainActivity", "All permissions for the location have been granted!")
            } else {
                Log.e("MainActivity", "Location permissions have not been granted!")
                showLocationPermissionExplanationDialog()
            }
        }
    }

    private fun showLocationPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("The app needs location permission to function properly, including for important functions such as emergency location.")
            .setPositiveButton("Try again") { dialog, _ ->
                dialog.dismiss()
                requestLocationPermissions()
            }
            .setNegativeButton("Give up") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}