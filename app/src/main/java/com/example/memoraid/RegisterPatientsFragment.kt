package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class RegisterPatientsFragment : Fragment() {

    private lateinit var caretakerCheckbox: CheckBox
    private lateinit var termsCheckbox: CheckBox
    private lateinit var finishButton: Button
    private lateinit var recyclerView: RecyclerView

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

        caretakerCheckbox.setOnCheckedChangeListener { _, isChecked ->
            recyclerView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        finishButton.setOnClickListener {
            if (termsCheckbox.isChecked) {
                // Navigate to the next step (e.g., success screen or dashboard)
                // Example: findNavController().navigate(R.id.action_registerPatientsFragment_to_homeFragment)
            } else {
                // Show an alert or toast to inform the user they need to agree to terms
            }
        }
    }
}
