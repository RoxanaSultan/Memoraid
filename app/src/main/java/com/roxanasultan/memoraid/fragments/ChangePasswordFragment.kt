package com.roxanasultan.memoraid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentChangePasswordBinding
import com.roxanasultan.memoraid.viewmodel.ChangePasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            showExitConfirmationDialog()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        binding.saveNewPasswordButton.setOnClickListener {
            val newPassword = binding.newPassword.text.toString()
            val confirmNewPassword = binding.confirmNewPassword.text.toString()

            when {
                newPassword.isEmpty() || confirmNewPassword.isEmpty() ->
                    Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()

                newPassword != confirmNewPassword ->
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()

                isPasswordValid(newPassword) != null ->
                    Toast.makeText(requireContext(), isPasswordValid(newPassword), Toast.LENGTH_SHORT).show()

                else -> viewModel.changePassword(newPassword)
            }
        }

        observeViewModel()

        return binding.root
    }

    private fun showExitConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to leave without saving?")
            .setPositiveButton("Yes") { _, _ ->
                findNavController().popBackStack()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.passwordChangeResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_changePasswordFragment_to_accountFragment)
                    } else {
                        Toast.makeText(requireContext(), "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                    viewModel.clearState()
                }
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
