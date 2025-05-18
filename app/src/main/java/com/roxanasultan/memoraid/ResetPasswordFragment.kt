package com.roxanasultan.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.roxanasultan.memoraid.databinding.FragmentResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordFragment : Fragment() {

    private lateinit var binding: FragmentResetPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle reset password logic when the button is clicked
        binding.resetPasswordButton.setOnClickListener {
            val newPassword = binding.editTextNewPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            // Validate the password fields
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(newPassword)) {
                Toast.makeText(requireContext(), "Password does not meet the requirements", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proceed with resetting the password
            resetPassword(newPassword)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        // Example password validation (you can modify it to fit your app's requirements)
        return password.length >= 8 // Password should be at least 8 characters
    }

    private fun resetPassword(newPassword: String) {
        // Get the current Firebase user
        val user = FirebaseAuth.getInstance().currentUser

        user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Password reset successfully!", Toast.LENGTH_SHORT).show()
                // Navigate back or show a success message
            } else {
                Toast.makeText(requireContext(), "Error resetting password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
