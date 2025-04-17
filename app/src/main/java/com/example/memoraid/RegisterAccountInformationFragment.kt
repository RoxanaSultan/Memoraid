package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterAccountInformationBinding
import com.example.memoraid.viewmodel.RegisterSharedViewModel
import com.example.memoraid.viewmodel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterAccountInformationFragment : Fragment() {

    private var _binding: FragmentRegisterAccountInformationBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RegisterSharedViewModel by activityViewModels()
    private val registerViewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    findNavController().navigate(R.id.action_registerAccountInfoFragment_to_registerOptionalAccountInfoFragment)
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

        val passwordError = isPasswordValid(password)
        if (passwordError != null) {
            Toast.makeText(requireContext(), passwordError, Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!registerViewModel.isUsernameUnique(username)) {
            Toast.makeText(requireContext(), "Username is already taken.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!registerViewModel.isEmailUnique(email)) {
            Toast.makeText(requireContext(), "Email is already registered.", Toast.LENGTH_SHORT).show()
            return false
        }

        sharedViewModel.setUsername(username)
        sharedViewModel.setEmail(email)
        sharedViewModel.setPassword(password)

        return true
    }

    private fun isPasswordValid(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain at least one lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain at least one digit"
            !password.any { "!@#\$%^&*()-_=+[{]}|;:'\",<.>/?`~".contains(it) } ->
                "Password must contain at least one special character"
            else -> null
        }
    }
}