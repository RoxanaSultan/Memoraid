package com.example.memoraid.fragments

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentAppointmentsBinding
import com.example.memoraid.models.Appointment
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.adapters.AppointmentAdapter
import com.example.memoraid.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        setupRecyclerView()
        loadUserData()

        return root
    }

    private fun setupRecyclerView() {
        binding.appointmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentRecyclerView.adapter = appointmentAdapter
        binding.appointmentRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        appointmentViewModel.loadUser()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appointmentViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.id?.let { userId ->
                                loadAppointments(date, userId)
                                appointmentAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadAppointments(date: String, userId: String) {
        appointmentViewModel.loadAppointments(date, userId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appointmentViewModel.appointments.collect { uploadedAppointments ->
                    appointments.clear()
                    appointments.addAll(uploadedAppointments)
                    appointmentAdapter.notifyDataSetChanged()

                    binding.noAppointmentsTextView.visibility = if (appointments.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}