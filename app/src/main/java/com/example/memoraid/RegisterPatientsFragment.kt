package com.example.memoraid

import RegisterViewModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterPatientsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterPatientsFragment : Fragment() {

    private lateinit var binding: FragmentRegisterPatientsBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Handle finish button click
        binding.registerFinishButton.setOnClickListener {
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(requireContext(), "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailValue = sharedViewModel.email.value
            val passwordValue = sharedViewModel.password.value

            registerUser(emailValue!!, passwordValue!!)
        }

        // Handle click event for Terms and Conditions link
        binding.linkTermsConditions.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.terms_conditions_url)))
            startActivity(intent)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sharedViewModel.clearData() // Clear all data stored in the ViewModel
                findNavController().navigateUp() // Navigates to the previous fragment
            }
        })
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserDetails()
                    sharedViewModel.clearData() // Clear all data stored in the ViewModel
                    findNavController().navigate(R.id.register_finish_button)
                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserDetails() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user?.uid ?: "")

        val username = sharedViewModel.username.value
        val email = sharedViewModel.email.value
        val firstName = sharedViewModel.firstName.value
        val lastName = sharedViewModel.lastName.value
        val phoneNumber = sharedViewModel.phoneNumber.value
        val birthdate = sharedViewModel.birthdate.value
        val profilePictureUrl = sharedViewModel.profilePictureUrl.value

        val userInfo = hashMapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "birthdate" to birthdate,
            "profilePictureUrl" to profilePictureUrl
        )

        userRef.set(userInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registration complete", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating user info: $e", Toast.LENGTH_SHORT).show()
            }
    }
}
