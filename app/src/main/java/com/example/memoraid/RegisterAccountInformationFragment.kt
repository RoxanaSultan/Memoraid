package com.example.memoraid

import RegisterViewModel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterAccountInformationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterAccountInformationFragment : Fragment() {

    private lateinit var binding: FragmentRegisterAccountInformationBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore // Firestore variable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sharedViewModel.clearData()
                findNavController().navigateUp()
            }
        })

        binding.firstRegisterContinueButton.setOnClickListener {
            val username = binding.registerUsername.text.toString().trim()
            val email = binding.registerEmailPhoneNumber.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val confirmPassword = binding.registerConfirmPassword.text.toString().trim()

            lifecycleScope.launch {
                if (validateInput(email, username, password, confirmPassword)) {
                    findNavController().navigate(R.id.fragment_register_optional_account_info)
                }
            }
        }
    }
    private suspend fun validateInput(
        email: String,
        username: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (username.trim().contains(" ")) {
            Toast.makeText(requireContext(), "Username cannot contain spaces.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 8) {
            Toast.makeText(requireContext(), "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isUsernameUnique(username)) {
            Toast.makeText(requireContext(), "Username is already taken.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isEmailUnique(email)) {
            Toast.makeText(requireContext(), "Email is already registered.", Toast.LENGTH_SHORT).show()
            return false
        }

        sharedViewModel.setUsername(username)
        sharedViewModel.setEmail(email)
        sharedViewModel.setPassword(password)

        return true
    }

    private suspend fun isUsernameUnique(username: String): Boolean {
        val querySnapshot = firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        return querySnapshot.isEmpty
    }

    private suspend fun isEmailUnique(email: String): Boolean {
        val querySnapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()
        return querySnapshot.isEmpty
    }
}