package com.example.memoraid

import android.graphics.Typeface
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
    fun sortAppointmentsByTime() {
        appointments.sortWith(compareBy { it.time })
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.appointmentName.text = appointment.name
            binding.appointmentDoctor.text = appointment.doctor ?: "-"
            binding.appointmentTime.text = appointment.time
            binding.appointmentLocation.text = appointment.location
            binding.appointmentCheckBox.isChecked = appointment.isCompleted

            updateLayout(appointment.isCompleted, binding)

            binding.appointmentCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateAppointmentStatus(appointment, isChecked)
                appointment.isCompleted = isChecked
                updateLayout(isChecked, binding)
            }
        }

        private fun updateAppointmentStatus(appointment: Appointment, isCompleted: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val appointmentRef = db.collection("appointments").document(appointment.id)

            appointmentRef.update("isCompleted", isCompleted)
                .addOnSuccessListener {
                    // Actualizarea s-a făcut cu succes
                    appointment.isCompleted = isCompleted
                }
                .addOnFailureListener {
                    // Tratează erorile dacă e cazul
                }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemAppointmentBinding) {
        if (completed) {
            binding.root.alpha = 0.5f
            binding.root.background = binding.root.context.getDrawable(R.drawable.completed_background)

            updateFontStyleLabels(Typeface.BOLD_ITALIC, binding)
            updateFontStyle(Typeface.ITALIC, binding)
        } else {
            binding.root.alpha = 1f
            binding.root.background = binding.root.context.getDrawable(R.drawable.layout_background)

            updateFontStyleLabels(Typeface.BOLD, binding)
            updateFontStyle(Typeface.NORMAL, binding)
        }
    }

    private fun updateFontStyle(style: Int, binding: ItemAppointmentBinding) {
        binding.appointmentName.setTypeface(null, style)
        binding.appointmentDoctor.setTypeface(null, style)
        binding.appointmentTime.setTypeface(null, style)
        binding.appointmentLocation.setTypeface(null, style)
    }

    private fun updateFontStyleLabels(style: Int, binding: ItemAppointmentBinding) {
        binding.appointmentLabelName.setTypeface(null, style)
        binding.appointmentLabelDoctor.setTypeface(null, style)
        binding.appointmentLabelTime.setTypeface(null, style)
        binding.appointmentLabelLocation.setTypeface(null, style)
    }
}