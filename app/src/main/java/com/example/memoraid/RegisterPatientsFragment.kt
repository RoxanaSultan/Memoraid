package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.viewModels.RegistrationViewModel
import com.google.firebase.auth.FirebaseAuth

class RegisterPatientsFragment : Fragment() {

    private lateinit var caretakerCheckbox: CheckBox
    private lateinit var termsCheckbox: CheckBox
    private lateinit var finishButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var sharedViewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_patients, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        caretakerCheckbox = view.findViewById(R.id.checkbox_caretaker)
        termsCheckbox = view.findViewById(R.id.checkbox_terms)
        finishButton = view.findViewById(R.id.register_finish_button)
        recyclerView = view.findViewById(R.id.recycler_view_patients_list)

        // Get the shared ViewModel
        sharedViewModel = activity?.let {
            androidx.lifecycle.ViewModelProvider(it)[RegistrationViewModel::class.java]
        }!!

        caretakerCheckbox.setOnCheckedChangeListener { _, isChecked ->
            recyclerView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        finishButton.setOnClickListener {
            // Get data from ViewModel
            val email = sharedViewModel.email
            val password = sharedViewModel.password
            if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Error: Missing data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Registration complete!", Toast.LENGTH_SHORT).show()
                    // Navigate to the next screen or finish the activity
                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
