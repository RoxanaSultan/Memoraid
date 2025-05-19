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

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val username = binding.loginUsername.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

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
                Toast.makeText(requireContext(), "Please enter your username.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            loginViewModel.sendResetEmail(username) { result ->
                result.onSuccess {
                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        loginViewModel.login(username, password)
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}