package com.roxanasultan.memoraid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.databinding.ItemExistingPatientBinding
import com.roxanasultan.memoraid.models.User

class ExistingPatientAdapter(
    private var patients: List<User?>,
    private val context: Context,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<ExistingPatientAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemExistingPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        if (patient != null) {
            holder.bind(patient)
        }
    }

    override fun getItemCount(): Int = patients.size

    inner class PatientViewHolder(val binding: ItemExistingPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: User) {
            binding.patientUsername.text = patient.username
            binding.patientName.text = patient.firstName + " " + patient.lastName
            binding.patientEmail.text = patient.email
            binding.patientPhone.text = patient.phoneNumber
//            binding.patientProfilePicture.setImageResource(patient.profilePicture)

            binding.deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(patient)
            }
        }

        private fun showDeleteConfirmationDialog(patient: User) {
            // Creăm un dialog de confirmare
            AlertDialog.Builder(context)
                .setTitle("Are you sure?")
                .setMessage("Do you really want to delete the patient?")
                .setPositiveButton("Yes") { _, _ ->
                    onDeleteClick(patient) // Se apelează callback-ul pentru ștergere
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    fun updatePatients(newPatients: List<User?>) {
        patients = newPatients
        notifyDataSetChanged()
    }
}