package com.example.memoraid

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
        private const val ALARM_PERMISSION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request necessary permissions
        requestNotificationPermission()
        checkAlarmPermission()

        // Set up navigation
        setupNavigation()

        // Log device timezone for debugging
        Log.d("TimeDebug", "Device timezone: ${TimeZone.getDefault().id}")
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Set default selection without checking savedInstanceState
        bottomNavigationView.selectedItemId = R.id.navigation_account
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Permission", "Notification permission already granted")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    showPermissionRationale(
                        "Notification Permission",
                        "This app needs notification permission to remind you about appointments.",
                        Manifest.permission.POST_NOTIFICATIONS,
                        NOTIFICATION_PERMISSION_CODE
                    )
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_CODE
                    )
                }
            }
        }
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                showAlarmPermissionDialog()
            }
        }
    }

    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exact Alarm Permission Needed")
            .setMessage("This app needs exact alarm permission to remind you about appointments at the exact time.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showPermissionRationale(
        title: String,
        message: String,
        permission: String,
        requestCode: Int
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "Notification permission granted")
                } else {
                    Log.d("Permission", "Notification permission denied")
                    showPermissionDeniedMessage("Notification")
                }
            }
        }
    }

    private fun showPermissionDeniedMessage(permissionName: String) {
        Toast.makeText(
            this,
            "$permissionName permission denied. Some features may not work properly.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onStart() {
        super.onStart()
        "onStart".logErrorMessage()
    }

    override fun onResume() {
        super.onResume()
        // Re-check alarm permission when returning to app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("Permission", "Exact alarm permission not granted")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        "onPause".logErrorMessage()
    }

    override fun onStop() {
        super.onStop()
        "onStop".logErrorMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        "onDestroy".logErrorMessage()
    }
}

fun String.logErrorMessage() {
    Log.e("Lifecycle", this)
}