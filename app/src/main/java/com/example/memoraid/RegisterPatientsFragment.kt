package com.example.memoraid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.adapters.PatientAdapter
import com.example.memoraid.databinding.FragmentRegisterAccountInformationBinding
import com.example.memoraid.databinding.FragmentRegisterPatientsBinding
import com.example.memoraid.models.Patient
import com.example.memoraid.viewmodel.RegisterSharedViewModel
import com.example.memoraid.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterPatientsFragment : Fragment() {

    private var _binding: FragmentRegisterPatientsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RegisterSharedViewModel by activityViewModels()
    private val registerViewModel: RegisterViewModel by viewModels()

    private lateinit var patientAdapter: PatientAdapter

    private val selectedPatients = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.checkboxCaretaker.setOnCheckedChangeListener { _, isChecked ->
            binding.recyclerViewPatientsList.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

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

            findNavController().navigate(R.id.action_registerPatientsFragment_to_loginFragment)
        }

        binding.linkTermsConditions.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.terms_conditions_url)))
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val patientsList = getPatients()

        patientAdapter = PatientAdapter(requireContext(), patientsList) { patient, isChecked ->
            if (isChecked) {
                selectedPatients.add(patient.id)
            } else {
                selectedPatients.remove(patient.id)
            }
        }

        binding.recyclerViewPatientsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPatientsList.adapter = patientAdapter
    }


    private fun getPatients(): List<Patient> {
        val patientsList = mutableListOf<Patient>()

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
                    patientsList.add(Patient(patientId, username, name, profilePictureUrl))
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