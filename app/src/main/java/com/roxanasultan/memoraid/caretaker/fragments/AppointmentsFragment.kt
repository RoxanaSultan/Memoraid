package com.roxanasultan.memoraid.caretaker.fragments

import SharedViewModel
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentAppointmentsCaretakerBinding
import com.roxanasultan.memoraid.models.Appointment
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.caretaker.adapters.AppointmentAdapter
import com.roxanasultan.memoraid.viewmodels.AppointmentsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.roxanasultan.memoraid.notifications.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.roxanasultan.memoraid.caretaker.adapters.PlaceAutocompleteAdapter

@AndroidEntryPoint
class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsCaretakerBinding? = null
    private val binding get() = _binding!!

    private val appointmentsViewModel: AppointmentsViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val appointments = mutableListOf<Appointment>()
    private var appointmentAdapter = AppointmentAdapter(appointments,
        onEditClick = { appointment -> showAddAppointmentDialog(appointment) },
        onDeleteClick = { appointment ->
            sharedViewModel.selectedDate.value?.let { date ->
                showDeleteOptionsDialog(appointment, date)
            }
        }
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
        appointmentsViewModel.loadUser()

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appointmentsViewModel.user.collect { user ->
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

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDoctor = dialogView.findViewById<EditText>(R.id.etDoctor)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerFrequency)
        val layoutEveryXDays = dialogView.findViewById<LinearLayout>(R.id.layoutEveryXDays)
        val layoutWeeklyDays = dialogView.findViewById<LinearLayout>(R.id.layoutWeeklyDays)
        val layoutMonthlyDay = dialogView.findViewById<LinearLayout>(R.id.layoutMonthlyDay)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        val etEveryXDays = dialogView.findViewById<EditText>(R.id.etEveryXDays)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        val autoCompleteLocation = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteLocation)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, getString(R.string.apiKey), Locale.getDefault())
        }

        val placesClient = Places.createClient(requireContext())
        val placesAdapter = PlaceAutocompleteAdapter(requireContext(), placesClient)
        autoCompleteLocation.setAdapter(placesAdapter)

        autoCompleteLocation.setOnItemClickListener { _, _, position, _ ->
            val item = placesAdapter.getItem(position)
            item?.let {
                val placeId = it.placeId
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

                val request = FetchPlaceRequest.newInstance(placeId, placeFields)
                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        val place = response.place
                        autoCompleteLocation.setText(place.address ?: place.name ?: "")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error fetching place: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        val frequencyOptions = resources.getStringArray(R.array.frequency_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, frequencyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        setupAppointmentTypeSpinner(spinnerType)

        appointment?.let {
            etTitle.setText(it.name)
            etDoctor.setText(it.doctor)
            autoCompleteLocation.setText(it.location)

            val appointmentTypes = resources.getStringArray(R.array.appointment_types)
            val selectedTypeIndex = appointmentTypes.indexOf(it.type)
            spinnerType.setSelection(selectedTypeIndex)

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
            val title = etTitle.text.toString().trim()
            val doctor = etDoctor.text.toString().trim()
            val location = autoCompleteLocation.text.toString().trim()
            val type = spinnerType.selectedItem.toString()
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
            val isValid = validateAppointmentInput(
                title,
                doctor,
                if (isDateRequired) date else "",
                time,
                location,
                type,
                isDateRequired
            )

            if (isValid) {
                val newAppointment = appointment?.copy(
                    id = appointment.id,
                    name = title,
                    doctor = doctor,
                    date = date,
                    time = time,
                    location = location,
                    type = type,
                    userId = appointment.userId,
                    completed = false,
                    frequency = frequency,
                    everyXDays = everyXDays,
                    weeklyDays = weeklyDays,
                    monthlyDay = monthlyDay,
                    skippedDates = emptyList(),
                    endDate = null
                ) ?: Appointment(
                    appointment?.id ?: "",
                    title,
                    doctor,
                    date,
                    time,
                    location,
                    type,
                    appointmentsViewModel.user.value?.selectedPatient ?: "",
                    false,
                    frequency = frequency,
                    everyXDays = everyXDays,
                    weeklyDays = weeklyDays,
                    monthlyDay = monthlyDay,
                    skippedDates = emptyList(),
                    endDate = null
                )

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
        type: String,
        isDateRequired: Boolean
    ): Boolean {
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (doctor.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a doctor name", Toast.LENGTH_SHORT).show()
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
            .setMessage("Are you sure you want to delete this appointment?")
            .setPositiveButton("Yes") { _, _ -> onConfirmed() }
            .setNegativeButton("No") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showDeleteOptionsDialog(appointment: Appointment, targetDate: String) {
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
                        skipThisOccurrence(appointment, targetDate)
                    }
                    1 -> showDeleteConfirmation {
                        endRepeatingAfter(appointment, targetDate)
                    }
                    2 -> showDeleteConfirmation {
                        deleteEntireSeries(appointment)
                    }
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun skipThisOccurrence(appointment: Appointment, targetDate: String) {
        val updated = appointment.copy(
            skippedDates = (appointment.skippedDates ?: emptyList()) + targetDate
        )
        saveAppointment(updated)
    }

    private fun endRepeatingAfter(appointment: Appointment, targetDate: String) {
        val updated = appointment.copy(endDate = targetDate)
        saveAppointment(updated)
    }

    private fun deleteEntireSeries(appointment: Appointment) {
        deleteAppointment(appointment)
    }

    private fun deleteAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            appointmentsViewModel.deleteAppointment(appointment.id) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                    loadAppointments(sharedViewModel.selectedDate.value ?: "",appointmentsViewModel.user.value?.selectedPatient ?: "")
                } else {
                    Toast.makeText(requireContext(), "Error deleting appointment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            if (appointment.id != null && appointment.id.isNotEmpty()) {
                appointmentsViewModel.updateAppointment(
                    appointment,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                        loadAppointments(sharedViewModel.selectedDate.value ?: "", appointmentsViewModel.user.value?.selectedPatient ?: "")
                        ReminderScheduler.scheduleReminder(requireContext(), appointment)
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error updating appointment", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                appointmentsViewModel.createAppointment(
                    appointment,
                    onSuccess = { id ->
                        Toast.makeText(requireContext(), "Appointment added successfully", Toast.LENGTH_SHORT).show()
                        loadAppointments(sharedViewModel.selectedDate.value ?: "", appointmentsViewModel.user.value?.selectedPatient ?: "")

                        ReminderScheduler.scheduleReminder(requireContext(), appointment.copy(id = id))
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error saving appointment", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun loadAppointments(date: String, userId: String) {
        appointmentsViewModel.loadAppointments(date, userId)

        lifecycleScope.launchWhenStarted {
            appointmentsViewModel.appointments.collect { uploadedAppointments ->
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