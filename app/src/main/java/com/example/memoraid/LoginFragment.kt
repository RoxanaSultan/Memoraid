package com.example.memoraid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.activities.MainActivity
import com.example.memoraid.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using View Binding
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth

        // Use binding to access views
        binding.loginButton.setOnClickListener {
//            val email = binding.loginUsername.text.toString().trim()
            val username = binding.loginUsername.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

//            if (validateInputs(email, password)) {
//                loginUser(email, password)
//            }

            if (validateInput(username, password)) {
                loginUser(username, password)
            }
        }

        binding.createAccountButton.setOnClickListener {
            findNavController().navigate(R.id.fragment_register_account_info)
        }

        binding.forgotPasswordButton.setOnClickListener {
            val username = binding.loginUsername.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your username.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firestore = FirebaseFirestore.getInstance()

            // Query Firestore to find the email by username
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val emailAddress = documents.documents[0].getString("email") ?: ""

                        // Proceed with sending the password reset email
                        FirebaseAuth.getInstance().sendPasswordResetEmail(emailAddress)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Username not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
//
//        binding.forgotPasswordButton.setOnClickListener {
//            val username = binding.loginUsername.text.toString().trim()
//
//            // Find the email associated with the username from Firestore
//            val db = FirebaseFirestore.getInstance()
//            val usersRef = db.collection("users") // Assuming you're storing user data in this collection
//
//            usersRef.whereEqualTo("username", username)
//                .get()
//                .addOnSuccessListener { documents ->
//                    if (!documents.isEmpty) {
//                        val userDoc = documents.first()
//                        val email = userDoc.getString("email") // Get the email address associated with the username
//
//                        // Send password reset email with custom URL
//                        val actionCodeSettings = ActionCodeSettings.newBuilder()
//                            .setUrl("https://memoraid.com/reset-password") // The URL that will handle the reset process
//                            .setHandleCodeInApp(true) // You want to handle the reset inside the app
//                            .build()
//
//                        if (email != null) {
//                            FirebaseAuth.getInstance().sendPasswordResetEmail(email, actionCodeSettings)
//                                .addOnCompleteListener { task ->
//                                    if (task.isSuccessful) {
//                                        Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                                    }
//                                }
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Username not found!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                .addOnFailureListener { exception ->
//                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
//                }
//        }

    }

//    private fun validateInputs(email: String, password: String): Boolean {
//        if (email.isEmpty()) {
//            Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        if (password.isEmpty()) {
//            Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        return true
//    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loginUser(username: String, password: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Query Firestore to find the email by username
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email") ?: ""
                    // Use the email to login
                    loginUserWithEmail(email, password)
                } else {
                    Toast.makeText(requireContext(), "Username not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loginUserWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Login failed
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


//    private fun loginUser(email: String, password: String) {
//        auth.signInWithEmailAndPassword(email, password)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    // Login successful, navigate to the next fragment or activity
//                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
//                    val intent = Intent(requireContext(), MainActivity::class.java)
//                    startActivity(intent)
//                } else {
//                    // If login fails, display error message
//                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//    }
}