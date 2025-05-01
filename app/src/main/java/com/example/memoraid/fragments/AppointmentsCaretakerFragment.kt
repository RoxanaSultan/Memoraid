package com.example.memoraid.fragments

import SharedViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentAppointmentsCaretakerBinding
import com.example.memoraid.models.Appointment
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.adapters.AppointmentCaretakerAdapter
import com.example.memoraid.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppointmentsCaretakerFragment : Fragment() {

    private var _binding: FragmentAppointmentsCaretakerBinding? = null
    private val binding get() = _binding!!

    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val appointments = mutableListOf<Appointment>()
    private var appointmentAdapter = AppointmentCaretakerAdapter(appointments,
        onEditClick = { appointment -> showAddAppointmentDialog(appointment) },
        onDeleteClick = { appointment -> showDeleteConfirmationDialog(appointment) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsCaretakerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        loadUserData()

        binding.addAppointmentButton.setOnClickListener {
            showAddAppointmentDialog()
        }

        return root
    }

    private fun setupRecyclerView() {
        binding.appointmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentRecyclerView.adapter = appointmentAdapter
        binding.appointmentRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        appointmentViewModel.loadUser()

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appointmentViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.selectedPatient?.let { patientId ->
                                loadAppointments(date, patientId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAddAppointmentDialog(appointment: Appointment? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_appointment, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (appointment == null) "Add New Appointment" else "Edit Appointment")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        // Initialize fields
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDoctor = dialogView.findViewById<EditText>(R.id.etDoctor)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etTime = dialogView.findViewById<EditText>(R.id.etTime)
        val etLocation = dialogView.findViewById<EditText>(R.id.etLocation)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        setupAppointmentTypeSpinner(spinnerType)

        // Fill the fields if editing
        appointment?.let {
            etTitle.setText(it.name)
            etDoctor.setText(it.doctor)
            etDate.setText(it.date)
            etTime.setText(it.time)
            etLocation.setText(it.location)
            val appointmentTypes = resources.getStringArray(R.array.appointment_types)
            val selectedTypeIndex = appointmentTypes.indexOf(it.type)
            spinnerType.setSelection(selectedTypeIndex)
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val doctor = etDoctor.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val type = spinnerType.selectedItem.toString()

            if (validateAppointmentInput(title, doctor, date, time, location, type)) {
                val newAppointment = appointment?.copy(
                    name = title,
                    doctor = doctor,
                    date = date,
                    time = time,
                    location = location,
                    type = type
                ) ?: Appointment(title, doctor, date, time, location, type)

                saveAppointment(newAppointment)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupAppointmentTypeSpinner(spinner: Spinner) {
        val appointmentTypes = resources.getStringArray(R.array.appointment_types)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            appointmentTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
    }

    private fun validateAppointmentInput(
        title: String,
        doctor: String,
        date: String,
        time: String,
        location: String,
        type: String
    ): Boolean {
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (doctor.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a doctor name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (date.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a date", Toast.LENGTH_SHORT).show()
            return false
        }
        if (time.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a time", Toast.LENGTH_SHORT).show()
            return false
        }
        if (location.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
            return false
        }
        if (type.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an appointment type", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validare format dată (dd-mm-yyyy)
        val datePattern = Regex("""^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)\d\d${'$'}""")
        if (!datePattern.matches(date)) {
            Toast.makeText(requireContext(), "Please enter a valid date (dd-mm-yyyy)", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validare format timp (hh:mm)
        val timePattern = Regex("""^([01][0-9]|2[0-3]):([0-5][0-9])${'$'}""")
        if (!timePattern.matches(time)) {
            Toast.makeText(requireContext(), "Please enter a valid time (hh:mm)", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showDeleteConfirmationDialog(appointment: Appointment) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this appointment?")
            .setPositiveButton("Yes") { _, _ ->
                deleteAppointment(appointment)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun deleteAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            appointmentViewModel.deleteAppointment(appointment.id) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                    loadAppointments(sharedViewModel.selectedDate.value ?: "",appointmentViewModel.user.value?.selectedPatient ?: "")
                } else {
                    Toast.makeText(requireContext(), "Error deleting appointment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            if (appointment.id != null && appointment.id.isNotEmpty()) {
                appointmentViewModel.updateAppointment(
                    appointment,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error updating appointment", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                appointmentViewModel.createAppointment(
                    appointment,
                    onSuccess = { id ->
                        Toast.makeText(requireContext(), "Appointment added successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error saving appointment", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun loadAppointments(date: String, userId: String) {
        appointmentViewModel.loadAppointments(date, userId)

        lifecycleScope.launchWhenStarted {
            appointmentViewModel.appointments.collect { uploadedAppointments ->
                appointments.clear()
                appointments.addAll(uploadedAppointments)

                appointmentAdapter.sortAppointmentsByTime()
                binding.appointmentRecyclerView.adapter?.notifyDataSetChanged()

                binding.noAppointmentsTextView.visibility = if (appointments.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}