package com.roxanasultan.memoraid.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.databinding.FragmentLoginBinding
import com.roxanasultan.memoraid.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val RC_SIGN_IN = 9001
    private val prefs by lazy { requireContext().getSharedPreferences("memoraid_prefs", MODE_PRIVATE) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        checkBiometricLogin()

        lifecycleScope.launchWhenStarted {
            loginViewModel.loginState.collect { result ->
                result?.onSuccess {
                    val email = firebaseAuth.currentUser?.email ?: return@onSuccess
                    val password = binding.loginPassword.text.toString().trim()

                    if (password.isNotEmpty()) {
                        saveCredentials(email, password)
                    }

                    if (!isBiometricEnabledForUser(email)) {
                        showEnableBiometricDialog(email)
                    } else {
                        navigateToMain()
                    }
                }?.onFailure {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.loginButton.setOnClickListener {
            val credential = binding.loginCredential.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()
            if (validateInput(credential, password)) {
                loginUser(credential, password)
            }
        }

        binding.googleLoginButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        binding.createAccountButton.setOnClickListener {
            findNavController().navigate(R.id.fragment_register_account_info)
        }

        binding.forgotPasswordButton.setOnClickListener {
            val credential = binding.loginCredential.text.toString().trim()
            if (credential.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your username, email or phone.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginViewModel.sendResetEmail(credential) { result ->
                result.onSuccess {
                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkBiometricLogin() {
        getSavedCredentials()?.let { (email, password) ->
            if (isBiometricEnabledForUser(email)) {
                showBiometricPrompt {
                    loginViewModel.login(email, password)
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Biometric auth failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun saveCredentials(email: String, password: String) {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "secret_prefs",
            masterKey,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPrefs.edit().putString("email", email).putString("password", password).apply()
    }

    private fun getSavedCredentials(): Pair<String, String>? {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "secret_prefs",
            masterKey,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val email = sharedPrefs.getString("email", null)
        val password = sharedPrefs.getString("password", null)
        return if (email != null && password != null) Pair(email, password) else null
    }

    private fun isBiometricEnabledForUser(userId: String): Boolean {
        return prefs.getBoolean("biometric_enabled_for_$userId", false)
    }

    private fun saveBiometricEnabled(userId: String) {
        prefs.edit().putBoolean("biometric_enabled_for_$userId", true).apply()
    }

    private fun showEnableBiometricDialog(userId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Enable biometric authentication?")
            .setMessage("Would you like to use fingerprint or face unlock for this account?")
            .setPositiveButton("Yes") { _, _ ->
                saveBiometricEnabled(userId)
                navigateToMain()
            }
            .setNegativeButton("No") { _, _ ->
                navigateToMain()
            }
            .show()
    }

    private fun loginUser(credential: String, password: String) {
        loginViewModel.login(credential, password)
    }

    private fun validateInput(credential: String, password: String): Boolean {
        if (credential.isEmpty()) {
            Toast.makeText(requireContext(), "Username, Email Address or Phone Number is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun navigateToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
