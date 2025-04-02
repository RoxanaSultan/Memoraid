package com.example.memoraid

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.databinding.FragmentAppointmentsBinding
import com.example.memoraid.models.Appointment
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.memoraid.adapters.AppointmentAdapter
import com.example.memoraid.helpers.AlarmHelper

class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String
    private lateinit var sharedViewModel: SharedViewModel

    private val appointmentList = mutableListOf<Appointment>()
    private val appointmentAdapter = AppointmentAdapter(appointmentList)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        database = FirebaseFirestore.getInstance()
        authenticator = FirebaseAuth.getInstance()
        currentUser = authenticator.currentUser?.uid ?: ""

        binding.appointmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentRecyclerView.adapter = appointmentAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            getAppointments(date)
        }
        binding.appointmentRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))

        return root
    }

    private fun getAppointments(date: String) {
        val appointments = database.collection("appointments")
            .whereEqualTo("userId", currentUser)
            .whereEqualTo("date", date)
            .get()

        appointments
            .addOnSuccessListener { documents ->
                appointmentList.clear()
                for (document in documents) {
                    val appointment = document.toObject(Appointment::class.java).apply {
                        id = document.id
                        isCompleted = document.get("isCompleted") as? Boolean ?: false
                    }
                    appointmentList.add(appointment)

                    // Set alarm for each appointment
                    context?.let {
                        AlarmHelper.setAlarm(it, appointment)
                    }
                }
                appointmentAdapter.sortAppointmentsByTime()
                appointmentAdapter.notifyDataSetChanged()

                binding.noAppointmentsTextView.visibility =
                    if (appointmentList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error getting documents: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}