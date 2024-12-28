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
import com.example.memoraid.databinding.FragmentRegisterOptionalAccountInformationBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterOptionalAccountInformationFragment : Fragment() {
    private lateinit var binding: FragmentRegisterOptionalAccountInformationBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize view binding
        binding = FragmentRegisterOptionalAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var selectedYear = 0
        var selectedMonth = 0
        var selectedDayOfMonth = 0

        // Capture the date from the CalendarView
        binding.registerBirthdate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDayOfMonth = dayOfMonth
        }

        // Handle the continue button click
        binding.secondRegisterContinueButton.setOnClickListener {
            val firstName = binding.registerFirstname.text.toString().trim()
            val lastName = binding.registerLastname.text.toString().trim()
            val phoneNumber = binding.registerPhoneNumber.text.toString().trim()

            // Format the selected date
            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthdate = dateFormat.format(calendar.time)

            // Pass data to the ViewModel
            sharedViewModel.setFirstName(firstName)
            sharedViewModel.setLastName(lastName)
            sharedViewModel.setPhoneNumber(phoneNumber)
            sharedViewModel.setBirthdate(birthdate)

            // Navigate to the next fragment
            findNavController().navigate(R.id.fragment_register_patients)
        }
    }



    /**
     * Validates user input for first and last name.
     */
}
