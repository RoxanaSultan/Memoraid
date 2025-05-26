package com.roxanasultan.memoraid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.databinding.FragmentLoginBinding
import com.roxanasultan.memoraid.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val RC_SIGN_IN = 9001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize GoogleSignInOptions with requestIdToken
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))  // important!
            .requestEmail()
            .build()

        // Initialize googleSignInClient as property, NOT local variable
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        lifecycleScope.launchWhenStarted {
            loginViewModel.loginState.collect { result ->
                result?.onSuccess {
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
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

//        binding.forgotPasswordButton.setOnClickListener {
//            val username = binding.loginUsername.text.toString().trim()
//
//            if (username.isEmpty()) {
//                Toast.makeText(requireContext(), "Please enter your username.", Toast.LENGTH_SHORT)
//                    .show()
//                return@setOnClickListener
//            }
//
//            loginViewModel.sendResetEmail(username) { result ->
//                result.onSuccess {
//                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
//                }.onFailure {
//                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = firebaseAuth.currentUser ?: return@addOnCompleteListener

                        // Verifici dacă userul e deja în Firestore pe baza emailului
                        lifecycleScope.launchWhenStarted {
                            loginViewModel.doesProfileExist(user.email ?: "") { exists ->
                                if (exists) {
                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    val bundle = Bundle().apply {
                                        putString("google_name", user.displayName)
                                        putString("google_email", user.email)
                                        putString("google_photo", user.photoUrl?.toString())
                                    }
                                    findNavController().navigate(R.id.action_loginFragment_to_registerDetailsFragment, bundle)
                                }
                            }
                        }

                    } else {
                        Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(credential: String, password: String) {
        loginViewModel.login(credential, password)
    }

    private fun validateInput(credential: String, password: String): Boolean {
        if (credential.isEmpty()) {
            Toast.makeText(requireContext(), "Username, Email or Phone Number is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}