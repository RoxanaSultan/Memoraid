package com.example.memoraid

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.memoraid.databinding.FragmentHealthBinding
import java.text.SimpleDateFormat
import java.util.*

class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Afișăm data curentă
        updateCurrentDate()

        // Click pe textul cu data → deschide DatePicker
        binding.currentDateText.setOnClickListener { openDatePicker() }

        // Navigare prin zile
        binding.previousDateButton.setOnClickListener { changeDate(-1) }
        binding.nextDateButton.setOnClickListener { changeDate(1) }

        return root
    }

    // Actualizează textul datei afișate
    private fun updateCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        binding.currentDateText.text = dateFormat.format(calendar.time)
    }

    // Deschide DatePickerDialog când apeși pe dată
    private fun openDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            updateCurrentDate()
        }, year, month, day).show()
    }

    // Schimbă ziua cu -1 sau +1
    private fun changeDate(days: Int) {
        calendar.add(Calendar.DAY_OF_MONTH, days)
        updateCurrentDate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}