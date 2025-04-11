package com.example.memoraid

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.databinding.FragmentAppointmentsBinding
import com.example.memoraid.models.Appointment
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.adapters.AppointmentAdapter
import com.example.memoraid.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppointmentsFragment : Fragment(R.layout.fragment_appointments) {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val appointments = mutableListOf<Appointment>()

    private var appointmentAdapter = AppointmentAdapter(appointments)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.appointmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentRecyclerView.adapter = appointmentAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            loadAppointments(date)
        }
        binding.appointmentRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))

        return root
    }

    private fun loadAppointments(date: String) {
        appointmentViewModel.loadAppointments(date)

        lifecycleScope.launchWhenStarted {
            appointmentViewModel.appointments.collect { uploadedAppointments ->
                appointments.clear()
                uploadedAppointments.forEach { appointment ->
                    appointments.add(appointment)
                }

                appointmentAdapter = AppointmentAdapter(appointments)
                appointmentAdapter.sortAppointmentsByTime()
                binding.appointmentRecyclerView.adapter = appointmentAdapter
                appointmentAdapter.notifyDataSetChanged()

                if (appointments.isEmpty()) {
                    binding.noAppointmentsTextView.visibility = View.VISIBLE
                } else {
                    binding.noAppointmentsTextView.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}