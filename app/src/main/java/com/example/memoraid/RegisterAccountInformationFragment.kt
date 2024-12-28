package com.example.memoraid

import RegisterViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterAccountInformationBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterAccountInformationFragment : Fragment() {

    private lateinit var binding: FragmentRegisterAccountInformationBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewBinding
        binding = FragmentRegisterAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up click listener for the continue button
        binding.firstRegisterContinueButton.setOnClickListener {
            val username = binding.registerUsername.text.toString().trim()
            val email = binding.registerEmailPhoneNumber.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val confirmPassword = binding.registerConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                // Call checkEmail and proceed based on the result
                checkEmail(email) { isValid ->
                    if (isValid) {
                        sharedViewModel.setUsername(username)
                        sharedViewModel.setEmail(email)
                        sharedViewModel.setPassword(password)
                        navigateToOptionalInfo()
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        when {
            email.isEmpty() -> {
                Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(requireContext(), "Invalid email", Toast.LENGTH_SHORT).show()
                return false
            }
            //TODO: check if the email is already registered
            password.isEmpty() -> {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
                return false
            }
            password != confirmPassword -> {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 8 -> {
                Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun checkEmail(email: String, callback: (Boolean) -> Unit) {
        binding.firstRegisterContinueButton.isEnabled = false // Disable button temporarily

        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                binding.firstRegisterContinueButton.isEnabled = true // Re-enable button

                if (task.isSuccessful) {
                    val result = task.result
                    if (result?.signInMethods?.isNotEmpty() == true) {
                        // Email is already registered
                        Toast.makeText(requireContext(), "This email is already registered.", Toast.LENGTH_LONG).show()
                        callback(false) // Call callback with false
                    } else {
                        // Email is valid and not registered
                        callback(true) // Call callback with true
                    }
                } else {
                    // Error occurred
                    val errorMessage = task.exception?.message ?: "An unknown error occurred."
                    Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    callback(false) // Call callback with false in case of error
                }
            }
            .addOnFailureListener { exception ->
                binding.firstRegisterContinueButton.isEnabled = true // Re-enable button
                val errorMessage = exception.localizedMessage ?: "An unknown error occurred."
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
                callback(false) // Call callback with false in case of failure
            }
    }

    private fun navigateToOptionalInfo() {
        findNavController().navigate(R.id.fragment_register_optional_account_info)
    }
}