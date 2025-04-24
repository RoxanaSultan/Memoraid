package com.example.memoraid.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        userViewModel.userRole.observe(this) { role ->
            bottomNavigationView.menu.clear()

            val navController = navHostFragment.navController
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
    }

    override fun onStart() {
        super.onStart()
        "onStart".logErrorMessage()
    }

    override fun onResume() {
        super.onResume()
        "onResume".logErrorMessage()
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