package com.roxanasultan.memoraid.fragments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.biometric.BiometricManager

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val RC_SIGN_IN = 9001

    private val prefs by lazy {
        requireContext().getSharedPreferences("memoraid_prefs", MODE_PRIVATE)
    }

    private val securePrefs by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secret_prefs",
            masterKey,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
                    // Sursa de adevăr! Utilizatorul proaspăt autentificat.
                    val user = firebaseAuth.currentUser ?: return@onSuccess
                    val email = user.email ?: return@onSuccess

                    // Salvează ultimul utilizator logat (pentru următoarea deschidere a aplicației)
                    prefs.edit().putString("last_logged_user", email).apply()

                    // Verifică dacă login-ul a fost făcut cu parolă
                    val passwordProvider = user.providerData.any { it.providerId == "password" }

                    if (passwordProvider) {
                        // Luăm parola din câmpul de text.
                        val passwordFromUi = binding.loginPassword.text.toString().trim()

                        // Metoda se salvează întotdeauna ca "password" pentru acest provider. Asta e corect.
                        saveLastLoginMethod(email, "password")

                        // AICI ESTE CONDIȚIA CHEIE:
                        // Salvăm credențialele DOAR dacă utilizatorul chiar a tastat o parolă.
                        // La un login biometric, `passwordFromUi` va fi gol, iar acest cod nu se va executa.
                        if (passwordFromUi.isNotEmpty()) {
                            saveCredentials(email, passwordFromUi)
                        }

                        // Restul logicii rămâne la fel.
                        if (!isBiometricEnabledForUser(email)) {
                            showEnableBiometricDialog(email)
                        } else {
                            navigateToMain()
                        }
                    } else {
                        // Acest caz nu ar trebui să fie atins prin loginViewModel,
                        // dar ca măsură de siguranță, navigăm direct
                        navigateToMain()
                    }

                }?.onFailure {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        var isPasswordVisible = false

        binding.passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.loginPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.passwordToggle.setImageResource(R.drawable.show_password)
            } else {
                binding.loginPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.passwordToggle.setImageResource(R.drawable.hide_password)
            }
            binding.loginPassword.setSelection(binding.loginPassword.text?.length ?: 0)
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

    private fun canDeviceUseBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("BiometricCheck", "App can authenticate using biometrics.")
                return true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e("BiometricCheck", "No biometric features available on this device.")
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e("BiometricCheck", "Biometric features are currently unavailable.")
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Utilizatorul nu și-a înregistrat nicio amprentă/față.
                // Poți afișa un Toast pentru a-l informa, dacă dorești.
                Log.e("BiometricCheck", "The user hasn't enrolled any biometrics.")
                return false
            }
            else -> {
                Log.e("BiometricCheck", "Biometric check returned an unknown status.")
                return false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val email = firebaseAuth.currentUser?.email
                            if (email != null) {
//                                securePrefs.edit().putString("email", email).apply()
                                saveLastLoginMethod(email, "google")
                                loginViewModel.doesProfileExist(email) { exists ->
                                    if (exists) {
                                        navigateToMain()
                                    } else {
                                        val lastAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
                                        val bundle = Bundle().apply {
                                            putString("google_name", lastAccount?.displayName ?: "")
                                            putString("google_email", lastAccount?.email ?: "")
                                            putString("google_photo", lastAccount?.photoUrl?.toString() ?: "")
                                        }
                                        findNavController().navigate(R.id.action_loginFragment_to_registerDetailsFragment, bundle)
                                    }
                                }
                            }
                        } else {
                            val exception = authResult.exception
                            if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                Toast.makeText(requireContext(),
                                    "An account associated with this email address already exists. Please log in using your email and password.",
                                    Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Google sign-in failed: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getGoogleSignInClientWithEmail(context: Context, email: String): GoogleSignInClient {
        val webClientId = getString(R.string.web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .setAccountName(email)
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    private fun checkBiometricLogin() {
        if (!canDeviceUseBiometrics()) return

        val email = prefs.getString("last_logged_user", null) ?: return
        Log.d("LoginFragment", "last_logged_user: $email")
        if (!isBiometricEnabledForUser(email)) return

        val lastMethod = getLastLoginMethod(email)
        Log.d("LoginFragment", "last_login_method: $lastMethod")

        if (lastMethod == "password") {
            val creds = getSavedCredentials()
            if (creds != null && creds.first == email) {
                showBiometricPrompt {
                    loginViewModel.login(creds.first, creds.second)
                }
            }
        } else if (lastMethod == "google") {
            showBiometricPrompt {
                val googleClient = getGoogleSignInClientWithEmail(requireContext(), email)
                val signInIntent = googleClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
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

                // ADAUGĂ ACEASTĂ SECȚIUNE
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Aici prinzi erorile precum "nu există date biometrice"
                    Toast.makeText(requireContext(),
                        "Authentication error: $errString",
                        Toast.LENGTH_LONG).show()
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
        securePrefs.edit().putString("email", email).putString("password", password).apply()
    }

    private fun getSavedCredentials(): Pair<String, String>? {
        val email = securePrefs.getString("email", null)
        val password = securePrefs.getString("password", null)
        return if (email != null && password != null) Pair(email, password) else null
    }

    private fun saveLastLoginMethod(email: String, method: String) {
        prefs.edit().putString("last_login_method_for_$email", method).apply()
    }

    private fun getLastLoginMethod(email: String): String? {
        return prefs.getString("last_login_method_for_$email", null)
    }

    private fun isBiometricEnabledForUser(userId: String): Boolean {
        return prefs.getBoolean("biometric_enabled_for_$userId", false)
    }

    private fun saveBiometricEnabled(userId: String, enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled_for_$userId", enabled).apply()
    }

    private fun showEnableBiometricDialog(userId: String) {
        if (!canDeviceUseBiometrics()) {
            // Dacă nu se poate, pur și simplu navighează mai departe fără a întreba.
            navigateToMain()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Enable biometric authentication?")
            .setMessage("Would you like to use fingerprint or face unlock for this account?")
            .setPositiveButton("Yes") { _, _ ->
                saveBiometricEnabled(userId, true)
                navigateToMain()
            }
            .setNegativeButton("No") { _, _ ->
                saveBiometricEnabled(userId, false)
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