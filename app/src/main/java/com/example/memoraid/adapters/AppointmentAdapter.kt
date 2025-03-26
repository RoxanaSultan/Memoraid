package com.example.memoraid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.databinding.ItemAppointmentBinding
import com.example.memoraid.models.Appointment
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentAdapter(private val appointments: MutableList<Appointment>) :
    RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val itemBinding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount(): Int = appointments.size

    inner class AppointmentViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.appointmentName.text = appointment.name
            binding.appointmentDoctor.text = appointment.doctor ?: "No doctor assigned"
            binding.appointmentTime.text = appointment.time
            binding.appointmentLocation.text = appointment.location

            binding.appointmentCheckBox.isChecked = appointment.isCompleted

            binding.appointmentCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateAppointmentStatus(appointment, isChecked)
            }
        }

        private fun updateAppointmentStatus(appointment: Appointment, isCompleted: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val appointmentRef = db.collection("appointments").document(appointment.id)

            appointmentRef.update("completed", isCompleted)
                .addOnSuccessListener {
                    appointment.isCompleted = isCompleted
                }
                .addOnFailureListener {
                }
        }
    }
}