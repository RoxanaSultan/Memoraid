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

        // Toggle RecyclerView visibility based on caretaker checkbox
        binding.checkboxCaretaker.setOnCheckedChangeListener { _, isChecked ->
            binding.recyclerViewPatientsList.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Handle finish button click
        binding.registerFinishButton.setOnClickListener {
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(requireContext(), "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailValue = sharedViewModel.email.value
            val passwordValue = sharedViewModel.password.value

            if (emailValue.isNullOrEmpty() || passwordValue.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong. Please restart the registration.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            registerUser(emailValue, passwordValue)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserDetails()
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

        // Gather all the user information from the ViewModel
        val username = sharedViewModel.username.value
        val email = sharedViewModel.email.value
        val firstName = sharedViewModel.firstName.value
        val lastName = sharedViewModel.lastName.value
        val caretakerStatus = binding.checkboxCaretaker.isChecked
        val patientsList = sharedViewModel.patientsList.value
        val role = if (caretakerStatus) "caretaker" else "patient"
        val phoneNumber = sharedViewModel.phoneNumber.value
        val birthdate = sharedViewModel.birthdate.value
        val profilePictureUrl = sharedViewModel.profilePictureUrl.value
        sharedViewModel.setRole(role)

        // Create a userInfo map with all the data
        val userInfo = hashMapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "patients" to patientsList,
            "role" to role,
            "phoneNumber" to phoneNumber,
            "birthdate" to birthdate,
            "profilePictureUrl" to profilePictureUrl
        )

        //TODO: Add a field for the user's profile picture URL
        //TODO: Upload the profile picture to Firebase Storage and get the download URL
        //TODO: Do not let the user proceed if there is an error

        // Save userInfo to Firestore
        userRef.set(userInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registration complete", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating user info: $e", Toast.LENGTH_SHORT).show()
            }
    }

}
