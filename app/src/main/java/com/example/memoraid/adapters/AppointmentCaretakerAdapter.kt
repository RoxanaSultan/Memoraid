package com.example.memoraid.adapters

import android.graphics.Typeface
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.databinding.ItemAppointmentCaretakerBinding
import com.example.memoraid.models.Appointment
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentCaretakerAdapter(
    private val appointments: MutableList<Appointment>,
    private val onEditClick: (Appointment) -> Unit,
    private val onDeleteClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentCaretakerAdapter.AppointmentViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val itemBinding = ItemAppointmentCaretakerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class AppointmentViewHolder(private val binding: ItemAppointmentCaretakerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.appointmentName.text = appointment.name
            binding.appointmentDoctor.text = appointment.doctor ?: "-"
            binding.appointmentTime.text = appointment.time
            binding.appointmentLocation.text = appointment.location
            binding.appointmentCheckBox.isChecked = appointment.completed

            updateLayout(appointment.completed, binding)

            binding.appointmentCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateAppointmentStatus(appointment, isChecked)
                appointment.completed = isChecked
                updateLayout(isChecked, binding)
            }

            binding.editButton.setOnClickListener {
                onEditClick(appointment)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(appointment)
            }
        }

        private fun updateAppointmentStatus(appointment: Appointment, completed: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val appointmentRef = db.collection("appointments").document(appointment.id)

            appointmentRef.update("completed", completed)
                .addOnSuccessListener {
                    appointment.completed = completed
                }
                .addOnFailureListener {
                }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemAppointmentCaretakerBinding) {
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

    private fun updateFontStyle(style: Int, binding: ItemAppointmentCaretakerBinding) {
        binding.appointmentName.setTypeface(null, style)
        binding.appointmentDoctor.setTypeface(null, style)
        binding.appointmentTime.setTypeface(null, style)
        binding.appointmentLocation.setTypeface(null, style)
        binding.appointmentCheckBox.setTypeface(null, style)
    }

    private fun updateFontStyleLabels(style: Int, binding: ItemAppointmentCaretakerBinding) {
        binding.appointmentLabelName.setTypeface(null, style)
        binding.appointmentLabelDoctor.setTypeface(null, style)
        binding.appointmentLabelTime.setTypeface(null, style)
        binding.appointmentLabelLocation.setTypeface(null, style)
        binding.appointmentLabelCheckBox.setTypeface(null, style)
    }
}