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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.adapters.PatientAdapter
import com.example.memoraid.databinding.FragmentRegisterPatientsBinding
import com.example.memoraid.models.Patient
import com.example.memoraid.models.PatientModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterPatientsFragment : Fragment() {

    private lateinit var binding: FragmentRegisterPatientsBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var patientAdapter: PatientAdapter

    // List to hold selected patients
    private val selectedPatients = mutableListOf<String>()

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

        // Set up RecyclerView
        setupRecyclerView()

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

            if (selectedPatients.isEmpty() && binding.checkboxCaretaker.isChecked) {
                Toast.makeText(requireContext(), "You must select at least one patient.", Toast.LENGTH_SHORT).show()
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

    private fun setupRecyclerView() {
        val patientsList = getPatients() // Replace with actual data source (Firebase, etc.)

        patientAdapter = PatientAdapter(requireContext(), patientsList) { patient, isChecked ->
            // Add or remove patient ID from selectedPatients list
            if (isChecked) {
                selectedPatients.add(patient.id)  // Add only the patient ID
            } else {
                selectedPatients.remove(patient.id)  // Remove patient ID
            }
        }

        binding.recyclerViewPatientsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPatientsList.adapter = patientAdapter
    }


    private fun getPatients(): List<PatientModel> {
        val patientsList = mutableListOf<PatientModel>()

        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        // Query to get users with the role "patient"
        usersCollection.whereEqualTo("role", "patient")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Iterate through the query results
                for (document in querySnapshot) {
                    val patientId = document.id
                    val username = document.getString("username")!!
                    val name = document.getString("firstName") + " " + document.getString("lastName")
                    val profilePictureUrl = document.getString("profilePictureUrl")
                    patientsList.add(PatientModel(patientId, username, name, profilePictureUrl))
                }

                patientAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching patients: $e", Toast.LENGTH_SHORT).show()
            }

        return patientsList
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
        val caretakerStatus = binding.checkboxCaretaker.isChecked
        val patientsList = selectedPatients // Use the selected patients list
        val role = if (caretakerStatus) "caretaker" else "patient"
        val phoneNumber = sharedViewModel.phoneNumber.value
        val birthdate = sharedViewModel.birthdate.value
        val profilePictureUrl = sharedViewModel.profilePictureUrl.value
        sharedViewModel.setRole(role)

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

        userRef.set(userInfo)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registration complete", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating user info: $e", Toast.LENGTH_SHORT).show()
            }
    }
}
