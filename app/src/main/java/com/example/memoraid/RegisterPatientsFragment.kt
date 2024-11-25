package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.viewModels.RegistrationViewModel
import com.google.firebase.auth.FirebaseAuth

class RegisterPatientsFragment : Fragment() {

    private lateinit var caretakerCheckbox: CheckBox
    private lateinit var termsCheckbox: CheckBox
    private lateinit var finishButton: Button
    private lateinit var recyclerView: RecyclerView
    private val sharedViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register_patients, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

        // Initialize views
        caretakerCheckbox = view.findViewById(R.id.checkbox_caretaker)
        termsCheckbox = view.findViewById(R.id.checkbox_terms)
        finishButton = view.findViewById(R.id.register_finish_button)
        recyclerView = view.findViewById(R.id.recycler_view_patients_list)

        // Toggle RecyclerView visibility based on caretaker checkbox
        caretakerCheckbox.setOnCheckedChangeListener { _, isChecked ->
            recyclerView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Handle finish button click
        finishButton.setOnClickListener {
            if (!termsCheckbox.isChecked) {
                Toast.makeText(requireContext(), "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = sharedViewModel.email
            val password = sharedViewModel.password

            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong. Please restart the registration.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                registerUser(email, password)
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.fragment_login)
                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}