package com.roxanasultan.memoraid.caretaker.fragments

import SharedViewModel
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.caretaker.adapters.MedicationAdapter
import com.roxanasultan.memoraid.databinding.FragmentMedicineCaretakerBinding
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodels.MedicationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MedicationFragment : Fragment() {

    private var _binding: FragmentMedicineCaretakerBinding? = null
    private val binding get() = _binding!!

    private val medicationViewModel: MedicationViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val medicationList = mutableListOf<Medicine>()
    private var medicationAdapter = MedicationAdapter(medicationList,
        onEditClick = { medication -> showAddMedicineDialog(medication) },
        onDeleteClick = { medication ->
            sharedViewModel.selectedDate.value?.let { date ->
                showDeleteOptionsDialog(medication, date)
            }
        }

    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineCaretakerBinding.inflate(inflater, container, false)
        val root = binding.root

        setupRecyclerView()
        loadUserData()

        binding.addMedicineButton.setOnClickListener {
            showAddMedicineDialog()
        }

        return root
    }

    private fun setupRecyclerView() {
        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.medicineRecyclerView.adapter = medicationAdapter
        binding.medicineRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        medicationViewModel.loadUser()

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicationViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.selectedPatient?.let { patientId ->
                                loadMedicine(date, patientId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadMedicine(date: String, patientId: String) {
        medicationViewModel.loadMedicine(date, patientId)

        viewLifecycleOwner.lifecycleScope.launch {
            medicationViewModel.medicine.collect { loadedMedicine ->
                medicationList.clear()
                medicationList.addAll(loadedMedicine)
                medicationAdapter.notifyDataSetChanged()

                binding.noMedicineTextView.visibility = if (medicationList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddMedicineDialog(medicine: Medicine? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_medicine, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (medicine == null) "Add New Medicine" else "Edit Medicine")
            .setView(dialogView)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val etDose = dialogView.findViewById<EditText>(R.id.etDose)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerFrequency)
        val layoutEveryXDays = dialogView.findViewById<LinearLayout>(R.id.layoutEveryXDays)
        val layoutWeeklyDays = dialogView.findViewById<LinearLayout>(R.id.layoutWeeklyDays)
        val layoutMonthlyDay = dialogView.findViewById<LinearLayout>(R.id.layoutMonthlyDay)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        val etEveryXDays = dialogView.findViewById<EditText>(R.id.etEveryXDays)

        val frequencyOptions = resources.getStringArray(R.array.frequency_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, frequencyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        medicine?.let {
            etName.setText(it.name)
            etDose.setText(it.dose)
            etNote.setText(it.note)

            it.date?.let { dateString ->
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = (parts[1].toIntOrNull() ?: 1) - 1
                    val year = parts[2].toIntOrNull() ?: 2025
                    datePicker.updateDate(year, month, day)
                }
            }

            val timeParts = it.time.split(":")
            if (timeParts.size == 2) {
                val hour = timeParts[0].toIntOrNull() ?: 0
                val minute = timeParts[1].toIntOrNull() ?: 0

                if (Build.VERSION.SDK_INT >= 23) {
                    timePicker.hour = hour
                    timePicker.minute = minute
                } else {
                    timePicker.currentHour = hour
                    timePicker.currentMinute = minute
                }
            }

            when (it.frequency) {
                "Once" -> datePicker.isEnabled = true
                "Every X days" -> layoutEveryXDays.visibility = View.VISIBLE
                "Weekly" -> layoutWeeklyDays.visibility = View.VISIBLE
                "Monthly" -> {
                    layoutMonthlyDay.visibility = View.VISIBLE
                    numberPicker.minValue = 1
                    numberPicker.maxValue = 31
                }
                else -> {
                    datePicker.isEnabled = false
                    layoutEveryXDays.visibility = View.GONE
                    layoutWeeklyDays.visibility = View.GONE
                    layoutMonthlyDay.visibility = View.GONE
                }
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()

                datePicker.isEnabled = false
                layoutEveryXDays.visibility = View.GONE
                layoutWeeklyDays.visibility = View.GONE
                layoutMonthlyDay.visibility = View.GONE

                when (selected) {
                    "Once" -> datePicker.isEnabled = true
                    "Every X days" -> layoutEveryXDays.visibility = View.VISIBLE
                    "Weekly" -> layoutWeeklyDays.visibility = View.VISIBLE
                    "Monthly" -> {
                        layoutMonthlyDay.visibility = View.VISIBLE
                        numberPicker.minValue = 1
                        numberPicker.maxValue = 31
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val dose = etDose.text.toString().trim()
            val note = etNote.text.toString().trim()
            val frequency = spinner.selectedItem.toString()

            val day = datePicker.dayOfMonth
            val month = datePicker.month + 1
            val year = datePicker.year
            val date = String.format("%02d-%02d-%04d", day, month, year)

            val hour = if (Build.VERSION.SDK_INT >= 23) timePicker.hour else timePicker.currentHour
            val minute = if (Build.VERSION.SDK_INT >= 23) timePicker.minute else timePicker.currentMinute
            val time = String.format("%02d:%02d", hour, minute)

            var everyXDays: Int? = null
            var weeklyDays: List<String>? = null
            var monthlyDay: Int? = null

            when (frequency) {
                "Every X days" -> {
                    val daysText = etEveryXDays.text.toString()
                    if (daysText.isNotEmpty()) everyXDays = daysText.toIntOrNull()
                }
                "Weekly" -> {
                    val selectedDays = mutableListOf<String>()
                    for (i in 0 until layoutWeeklyDays.childCount) {
                        val child = layoutWeeklyDays.getChildAt(i)
                        if (child is CheckBox && child.isChecked) {
                            selectedDays.add(child.text.toString().uppercase())
                        }
                    }
                    weeklyDays = selectedDays
                }
                "Monthly" -> {
                    monthlyDay = numberPicker.value
                }
            }

            if (frequency == "Every X days" && everyXDays == null) {
                Toast.makeText(requireContext(), "Please enter a valid number of days", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (frequency == "Weekly" && (weeklyDays.isNullOrEmpty())) {
                Toast.makeText(requireContext(), "Please select at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isDateRequired = frequency == "Once"
            val isValid = validateMedicineInput(
                name,
                if (isDateRequired) date else "",
                time,
                dose,
                note,
                isDateRequired
            )

            if (isValid) {
                val newMedicine = medicine?.copy(
                    name = name,
                    date = date,
                    time = time,
                    dose = dose,
                    note = note,
                    frequency = frequency,
                    everyXDays = everyXDays,
                    weeklyDays = weeklyDays,
                    monthlyDay = monthlyDay,
                    skippedDates = emptyList(),
                    endDate = null
                ) ?: Medicine(
                    name = name,
                    date = date,
                    time = time,
                    dose = dose,
                    note = note,
                    userId = medicationViewModel.user.value?.selectedPatient ?: "",
                    frequency = frequency,
                    everyXDays = everyXDays,
                    weeklyDays = weeklyDays,
                    monthlyDay = monthlyDay,
                    skippedDates = emptyList(),
                    endDate = null
                )

                saveMedicine(newMedicine)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateMedicineInput(
        name: String,
        date: String,
        time: String,
        dose: String,
        note: String,
        isDateRequired: Boolean = true
    ): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter medicine name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (dose.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter dose", Toast.LENGTH_SHORT).show()
            return false
        }
        if (note.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter note", Toast.LENGTH_SHORT).show()
            return false
        }
        if (isDateRequired) {
            if (date.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter date", Toast.LENGTH_SHORT).show()
                return false
            }
            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val dateTimeString = "$date $time"
            val selectedDateTime = try {
                formatter.parse(dateTimeString)
            } catch (e: Exception) {
                null
            }
            if (selectedDateTime != null) {
                val now = Calendar.getInstance().time
                if (selectedDateTime.before(now)) {
                    Toast.makeText(requireContext(), "Date and time must not be in the past", Toast.LENGTH_SHORT).show()
                    return false
                }
            } else {
                Toast.makeText(requireContext(), "Invalid date/time format", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun showDeleteConfirmation(onConfirmed: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this medication?")
            .setPositiveButton("Yes") { _, _ -> onConfirmed() }
            .setNegativeButton("No") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showDeleteOptionsDialog(medicine: Medicine, targetDate: String) {
        val options = arrayOf(
            "Delete only this occurrence",
            "Delete this and following",
            "Delete all"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Medicine")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDeleteConfirmation {
                        skipThisOccurrence(medicine, targetDate)
                    }
                    1 -> showDeleteConfirmation {
                        endRepeatingAfter(medicine, targetDate)
                    }
                    2 -> showDeleteConfirmation {
                        deleteEntireSeries(medicine)
                    }
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun skipThisOccurrence(medicine: Medicine, targetDate: String) {
        val updated = medicine.copy(
            skippedDates = (medicine.skippedDates ?: emptyList()) + targetDate
        )
        saveMedicine(updated)
    }

    private fun endRepeatingAfter(medicine: Medicine, targetDate: String) {
        val updated = medicine.copy(endDate = targetDate)
        saveMedicine(updated)
    }

    private fun deleteEntireSeries(medicine: Medicine) {
        deleteMedicine(medicine)
    }

    private fun deleteMedicine(medicine: Medicine) {
        lifecycleScope.launch {
            medicationViewModel.deleteMedicine(medicine.id) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "Medicine deleted successfully", Toast.LENGTH_SHORT).show()
                    loadMedicine(sharedViewModel.selectedDate.value ?: "", medicationViewModel.user.value?.selectedPatient ?: "")
                } else {
                    Toast.makeText(requireContext(), "Error deleting medicine", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveMedicine(medicine: Medicine) {
        lifecycleScope.launch {
            if (medicine.id.isNotEmpty()) {
                medicationViewModel.updateMedicine(
                    medicine,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Medicine updated successfully", Toast.LENGTH_SHORT).show()
                        loadMedicine(sharedViewModel.selectedDate.value ?: "", medicationViewModel.user.value?.selectedPatient ?: "")
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error updating Medicine", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                medicationViewModel.addMedicine(
                    medicine,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Medicine added successfully", Toast.LENGTH_SHORT).show()
                        loadMedicine(sharedViewModel.selectedDate.value ?: "", medicationViewModel.user.value?.selectedPatient ?: "")
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error saving medicine", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}