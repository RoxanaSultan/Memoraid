package com.roxanasultan.memoraid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.helpers.BiometricHelper
import com.roxanasultan.memoraid.viewmodels.AuthenticationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val authenticationViewModel: AuthenticationViewModel by viewModels()

    private lateinit var biometricHelper: BiometricHelper
    private val prefs by lazy { getSharedPreferences("memoraid_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val intent = Intent(this@AuthenticationActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val lastUser = getLastLoggedUser()

        if (lastUser != null && isBiometricEnabledForUser(lastUser)) {
            showAuthLayout()
            startBiometricAuth()
        } else {
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

    private fun startBiometricAuth() {
        biometricHelper = BiometricHelper(this, object : BiometricHelper.AuthenticationCallback {
            override fun onSuccess() {
                val lastUser = getLastLoggedUser()
                if (lastUser != null) {
                    val intent = Intent(this@AuthenticationActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showAuthLayout()
                }
            }

            override fun onError(errorCode: Int, errString: CharSequence) {
                showAuthLayout()
            }

            override fun onFailed() {
                showAuthLayout()
            }
        })

        biometricHelper.authenticate()
    }

    private fun getLastLoggedUser(): String? {
        return prefs.getString("last_logged_user", null)
    }

    private fun isBiometricEnabledForUser(userId: String): Boolean {
        return prefs.getBoolean("biometric_enabled_for_$userId", false)
    }
}