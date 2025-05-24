package com.roxanasultan.memoraid.fragments

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.MainActivity
import com.roxanasultan.memoraid.patient.adapters.PatientAdapter
import com.roxanasultan.memoraid.databinding.FragmentRegisterPatientsBinding
import com.roxanasultan.memoraid.models.Patient
import com.roxanasultan.memoraid.viewmodels.RegisterSharedViewModel
import com.roxanasultan.memoraid.viewmodels.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        registerViewModel.getPatients()

        lifecycleScope.launchWhenStarted {
            registerViewModel.patients.collect { list ->
                if (list.isNotEmpty()) {
                    setupRecyclerView(list)
                }
            }
        }

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

            if (isGoogleSignIn()) {
                lifecycleScope.launch {
                    saveUserDetails()
                    Toast.makeText(requireContext(), "Registration completed via Google", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            } else {
                if (emailValue.isNullOrEmpty() || passwordValue.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                registerUser(emailValue, passwordValue)
            }
        }

        binding.linkTermsConditions.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.terms_conditions_url)))
            startActivity(intent)
        }
    }

    private fun isGoogleSignIn(): Boolean {
        return sharedViewModel.password.value.isNullOrEmpty()
    }

    private fun setupRecyclerView(patientsList: List<Patient>) {
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

    private fun registerUser(email: String, password: String) {
        registerViewModel.registerUser(email, password) { isSuccess, errorMessage ->
            if (isSuccess) {
                lifecycleScope.launch {
                    saveUserDetails()
                    findNavController().navigate(R.id.action_registerPatientsFragment_to_loginFragment)
                }
            } else {
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun saveUserDetails() {
        val username = sharedViewModel.username.value
        val email = sharedViewModel.email.value
        val firstName = sharedViewModel.firstName.value
        val lastName = sharedViewModel.lastName.value
        val caretakerStatus = binding.checkboxCaretaker.isChecked
        val role = if (caretakerStatus) "caretaker" else "patient"
        val phoneNumber = sharedViewModel.phoneNumber.value
        val birthdate = sharedViewModel.birthdate.value
        val profilePictureUrl = sharedViewModel.profilePictureUrl.value
        val selectedPatientsList = selectedPatients

        sharedViewModel.setSelectedPatient(selectedPatients[0])
        sharedViewModel.setRole(role)

        val userInfo = hashMapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "birthdate" to birthdate,
            "profilePictureUrl" to profilePictureUrl,
            "role" to role,
            "selectedPatient" to selectedPatientsList.firstOrNull(),
            "caretakers" to emptyList<String>(),
            "patients" to emptyList<String>()
        )

        if (role == "patient") {
            userInfo["caretakers"] = emptyList<String>()
            userInfo["patients"] = null
        } else {
            userInfo["patients"] = selectedPatientsList
            userInfo["caretakers"] = null

            selectedPatientsList.forEach { patientId ->
                val patient = registerViewModel.getUserById(patientId)
                if (patient != null) {
                    val updatedCaretakers = patient.caretakers?.toMutableList()
                    updatedCaretakers?.add(registerViewModel.getCurrentUser()?.uid!!)
                    registerViewModel.updatePatientCaretakers(patientId, updatedCaretakers)
                }
            }
        }

        lifecycleScope.launch {
            val uid = registerViewModel.getCurrentUser()?.uid ?: return@launch
            registerViewModel.saveUserDetails(uid, userInfo) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "User details saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error saving user details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}