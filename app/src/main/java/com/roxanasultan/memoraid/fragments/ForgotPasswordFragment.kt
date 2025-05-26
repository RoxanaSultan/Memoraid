package com.roxanasultan.memoraid.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentForgotPasswordBinding
import com.roxanasultan.memoraid.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sendLinkButton.setOnClickListener {
            val credential = binding.inputCredential.text.toString().trim()
            if (credential.isEmpty()) {
                Toast.makeText(requireContext(), "Username, Email or Phone Number is required", Toast.LENGTH_SHORT).show()
            } else {
                when {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(credential).matches() -> {
                        // Credential este deja un email
                        sendEmail(credential)
                    }
                    credential.all { it.isDigit() } && credential.length in 7..15 -> {
                        // Este un număr de telefon, trebuie să obținem email-ul asociat
                        loginViewModel.getEmailByPhoneNumber(credential) { email ->
                            if (!email.isNullOrEmpty()) {
                                sendEmail(email)
                            } else {
                                Toast.makeText(requireContext(), "Email not found for this phone number!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else -> {
                        // Presupunem că credential-ul este un username și căutăm email-ul
                        loginViewModel.getEmailByUsername(credential) { email ->
                            if (!email.isNullOrEmpty()) {
                                sendEmail(email)
                            } else {
                                Toast.makeText(requireContext(), "Email not found for this username!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        binding.goBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }

    private fun sendEmail(email: String?) {
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid email address!", Toast.LENGTH_SHORT).show()
            Log.e("ForgotPasswordFragment", "Email is null or empty!")
            return
        }

        Log.e("ForgotPasswordFragment", "Email: $email")

        val resetLink = "https://example.com/reset-password?user=$email"
        val autoLoginLink = "https://example.com/auto-login?token=SESSION_TOKEN"

        val subject = "Reset Your Password or Log In Instantly"
        val body = """
        Click one of the options below:
        - Reset Password: $resetLink
        - Log In Instantly: $autoLoginLink
    """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Send Email"))
            Toast.makeText(requireContext(), "Email sent to $email!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No email client found.", Toast.LENGTH_SHORT).show()
            Log.e("ForgotPasswordFragment", "No email client installed!")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}