package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.viewModels.RegistrationViewModel

class RegisterAccountInformationFragment : Fragment() {

    private lateinit var sharedViewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_account_information, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the shared ViewModel
        sharedViewModel = activity?.let {
            androidx.lifecycle.ViewModelProvider(it)[RegistrationViewModel::class.java]
        }!!

        val emailField: EditText = view.findViewById(R.id.register_email_phone_number)
        val passwordField: EditText = view.findViewById(R.id.register_password)
        val confirmPasswordField: EditText = view.findViewById(R.id.register_confirm_password)
        val continueButton: Button = view.findViewById(R.id.first_register_continue_button)

        continueButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (validateInputs(email, password, confirmPassword)) {
                // Save email and password in shared ViewModel
                sharedViewModel.email = email
                sharedViewModel.password = password

                // Navigate to the next fragment
                findNavController().navigate(R.id.fragment_register_optional_account_info)
            }
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}