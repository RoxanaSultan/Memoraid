package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.viewModels.RegistrationViewModel
import com.google.firebase.auth.FirebaseAuth

class RegisterAccountInformationFragment : Fragment() {

    private val sharedViewModel: RegistrationViewModel by activityViewModels() // Shared ViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register_account_information, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth

        val emailField: EditText = view.findViewById(R.id.register_email_phone_number)
        val passwordField: EditText = view.findViewById(R.id.register_password)
        val confirmPasswordField: EditText = view.findViewById(R.id.register_confirm_password)
        val continueButton: Button = view.findViewById(R.id.first_register_continue_button)

        continueButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (validateInputs(email, password, confirmPassword)) {
                checkEmailAndNavigate(email, password, continueButton)
            }
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(
                requireContext(),
                "Password must be at least 8 characters",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    // TODO: check if the email is indeed real
    // TODO: check if the email is already registered
    private fun checkEmailAndNavigate(email: String, password: String, continueButton: Button) {
        continueButton.isEnabled = false // Disable button to prevent multiple clicks during processing

        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                continueButton.isEnabled = true // Re-enable button after the task is completed

                if (task.isSuccessful) {
                    val result = task.result
                    if (result?.signInMethods?.isNotEmpty() == true) {
                        // Email is already registered
                        Toast.makeText(requireContext(), "This email is already registered.", Toast.LENGTH_LONG).show()
                    } else {
                        // Email is valid and not registered
                        sharedViewModel.email = email
                        sharedViewModel.password = password
                        findNavController().navigate(R.id.fragment_register_optional_account_info)
                    }
                } else {
                    // Error occurred
                    val errorMessage = task.exception?.message ?: "An unknown error occurred."
                    Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                continueButton.isEnabled = true // Re-enable button in case of failure
                val errorMessage = exception.localizedMessage ?: "An unknown error occurred."
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
    }
}
