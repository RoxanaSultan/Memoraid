package com.roxanasultan.memoraid.fragments

import SharedViewModel
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.roxanasultan.memoraid.adapters.HealthObservationAdapter
import com.roxanasultan.memoraid.databinding.FragmentHealthCaretakerBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*

class HealthObservationFragment : Fragment() {

    private var _binding: FragmentHealthCaretakerBinding? = null
    private val binding get() = _binding!!
    private val calendar: Calendar = Calendar.getInstance()
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthCaretakerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        updateCurrentDate()

        binding.currentDateText.setOnClickListener { openDatePicker() }

        binding.previousDateButton.setOnClickListener { changeDate(-1) }
        binding.nextDateButton.setOnClickListener { changeDate(1) }

        val adapter = HealthObservationAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Appointments"
                1 -> "Medicine"
                2 -> "Habits"
                else -> ""
            }
        }.attach()

        return root
    }

    private fun updateCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        binding.currentDateText.text = dateFormat.format(calendar.time)

        val selectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
        sharedViewModel.setSelectedDate(selectedDate)
    }

    private fun openDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            updateCurrentDate()
        }, year, month, day).show()
    }

    private fun changeDate(days: Int) {
        calendar.add(Calendar.DAY_OF_MONTH, days)
        updateCurrentDate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}