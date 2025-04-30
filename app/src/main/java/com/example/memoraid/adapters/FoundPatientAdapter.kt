package com.example.memoraid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.databinding.ItemFoundPatientBinding
import com.example.memoraid.models.User

class FoundPatientAdapter(
    private var patients: List<User>, // Schimbat din List<User?>
    private val context: Context,
    private val onAddClick: (User) -> Unit
) : RecyclerView.Adapter<FoundPatientAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemFoundPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        holder.bind(patient) // Nu mai e nevoie de verificare null
    }

    override fun getItemCount(): Int = patients.size

    inner class PatientViewHolder(private val binding: ItemFoundPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: User) {
            binding.patientUsername.text = patient.username ?: "N/A"
            binding.patientName.text = "${patient.firstName ?: ""} ${patient.lastName ?: ""}".trim()
            binding.patientEmail.text = patient.email ?: "N/A"
            binding.patientPhone.text = patient.phoneNumber ?: "N/A"

            binding.addButton.setOnClickListener {
                showAddConfirmationDialog(patient)
            }
        }

        private fun showAddConfirmationDialog(patient: User) {
            AlertDialog.Builder(context)
                .setTitle("Add Patient")
                .setMessage("Do you want to assign this patient to your care?")
                .setPositiveButton("Yes") { _, _ ->
                    onAddClick(patient)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    fun updatePatients(newPatients: List<User>) {
        patients = newPatients
        notifyDataSetChanged()
    }
}