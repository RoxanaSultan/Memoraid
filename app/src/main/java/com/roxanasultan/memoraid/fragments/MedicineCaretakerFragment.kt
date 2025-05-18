package com.roxanasultan.memoraid.fragments

import SharedViewModel
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.roxanasultan.memoraid.adapters.MedicineCaretakerAdapter
import com.roxanasultan.memoraid.databinding.FragmentMedicineCaretakerBinding
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.receivers.AlarmReceiver
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodel.MedicineViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MedicineCaretakerFragment : Fragment() {

    private var _binding: FragmentMedicineCaretakerBinding? = null
    private val binding get() = _binding!!

    private val medicineViewModel: MedicineViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val medicineList = mutableListOf<Medicine>()
    private var medicineAdapter = MedicineCaretakerAdapter(medicineList,
        onEditClick = { medicine -> showAddMedicineDialog(medicine) },
        onDeleteClick = { medicine -> showDeleteConfirmationDialog(medicine) }
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
        binding.medicineRecyclerView.adapter = medicineAdapter
        binding.medicineRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        medicineViewModel.loadUser()

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicineViewModel.user.collect { user ->
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
        medicineViewModel.loadMedicine(date, patientId)

        viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.medicine.collect { loadedMedicine ->
                medicineList.clear()
                medicineList.addAll(loadedMedicine)
                medicineAdapter.notifyDataSetChanged()

                binding.noMedicineTextView.visibility = if (medicineList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun scheduleMedicineReminder(hour: Int, minute: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1) // setează pe ziua următoare dacă ora a trecut deja
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
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
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etTime = dialogView.findViewById<EditText>(R.id.etTime)
        val etDose = dialogView.findViewById<EditText>(R.id.etDose)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        medicine?.let {
            etName.setText(it.name)
            etDate.setText(it.date)
            etTime.setText(it.time)
            etDose.setText(it.dose)
            etNote.setText(it.note)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()
            val dose = etDose.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (validateMedicineInput(name, date, time, dose, note)) {
                val newMedicine = medicine?.copy(
                    id = medicine.id,
                    name = name,
                    date = date,
                    time = time,
                    dose = dose,
                    note = note,
                    userId = medicine.userId,
                    taken = medicine.taken
                ) ?: Medicine(
                    medicine?.id ?: "",
                    name,
                    date,
                    time,
                    dose,
                    note,
                    medicineViewModel.user.value?.selectedPatient ?: "",
                    false
                )

                saveMedicine(newMedicine)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateMedicineInput(name: String, date: String, time: String, dose: String, note: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter medicine name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (date.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter date", Toast.LENGTH_SHORT).show()
            return false
        }
        if (time.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter time", Toast.LENGTH_SHORT).show()
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

        val datePattern = Regex("""^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)\d\d${'$'}""")
        if (!datePattern.matches(date)) {
            Toast.makeText(requireContext(), "Please enter a valid date (dd-mm-yyyy)", Toast.LENGTH_SHORT).show()
            return false
        }

        val timePattern = Regex("""^([01][0-9]|2[0-3]):([0-5][0-9])${'$'}""")
        if (!timePattern.matches(time)) {
            Toast.makeText(requireContext(), "Please enter valid time (hh:mm)", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showDeleteConfirmationDialog(medicine: Medicine) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this medicine?")
            .setPositiveButton("Yes") { _, _ -> deleteMedicine(medicine) }
            .setNegativeButton("No") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun deleteMedicine(medicine: Medicine) {
        lifecycleScope.launch {
            medicineViewModel.deleteMedicine(medicine.id) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "Medicine deleted successfully", Toast.LENGTH_SHORT).show()
                    loadMedicine(sharedViewModel.selectedDate.value ?: "", medicineViewModel.user.value?.selectedPatient ?: "")
                } else {
                    Toast.makeText(requireContext(), "Error deleting medicine", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveMedicine(medicine: Medicine) {
        lifecycleScope.launch {
            if (medicine.id != null && medicine.id.isNotEmpty()) {
                medicineViewModel.updateMedicine(
                    medicine,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Medicine updated successfully", Toast.LENGTH_SHORT).show()
                        loadMedicine(sharedViewModel.selectedDate.value ?: "", medicineViewModel.user.value?.selectedPatient ?: "")
                        val hour = medicine.time.split(":")[0].toInt()
                        val minute = medicine.time.split(":")[1].toInt()
                        scheduleMedicineReminder(hour, minute)
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error updating Medicine", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                medicineViewModel.addMedicine(
                    medicine,
                    onSuccess = { id ->
                        Toast.makeText(requireContext(), "Medicine added successfully", Toast.LENGTH_SHORT).show()
                        loadMedicine(sharedViewModel.selectedDate.value ?: "", medicineViewModel.user.value?.selectedPatient ?: "")
                        val hour = medicine.time.split(":")[0].toInt()
                        val minute = medicine.time.split(":")[1].toInt()
                        scheduleMedicineReminder(hour, minute)
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