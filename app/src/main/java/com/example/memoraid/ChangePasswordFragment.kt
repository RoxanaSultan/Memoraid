package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the binding layout
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        // Set up the button click listener using binding
        binding.saveNewPasswordButton.setOnClickListener {
            val newPassword = binding.newPassword.text.toString()
            val confirmNewPassword = binding.confirmNewPassword.text.toString()

            // Validate the inputs
            if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else if (newPassword.length < 8) {
                Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmNewPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Proceed with password change logic (e.g., update password in Firestore or local storage)
                saveNewPassword(newPassword)
            }
        }

        return binding.root
    }

    private fun saveNewPassword(newPassword: String) {
        // Get the current user
        val user = auth.currentUser

        if (user != null) {
            // Update the user's password in Firebase Authentication
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Password updated successfully
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()

                        // Navigate back to the account fragment or any other fragment
                        findNavController().navigate(R.id.action_changePasswordFragment_to_accountFragment)
                    } else {
                        // If password update fails, show an error
                        Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // If no user is logged in
            Toast.makeText(requireContext(), "No user is logged in", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Set the binding to null when the view is destroyed to avoid memory leaks
        _binding = null
    }
}