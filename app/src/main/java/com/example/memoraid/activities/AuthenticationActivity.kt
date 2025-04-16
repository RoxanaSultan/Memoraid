package com.example.memoraid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.memoraid.R
import com.example.memoraid.viewmodel.AuthenticationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val authenticationViewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            authenticationViewModel.authCheckState.collectLatest { result ->
                if (result != null) {
                    if (result.isSuccess) {
                        val intent = Intent(this@AuthenticationActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        showAuthLayout()
                    }
                }
            }
        }

        authenticationViewModel.checkIfUserLoggedIn()
    }

    private fun showAuthLayout() {
        setContentView(R.layout.activity_authentication)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val data: Uri? = intent.data
        if (data != null && data.path == "/reset-password") {
            navController.navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
    }
}