package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.databinding.ItemFoundPatientBinding
import com.example.memoraid.models.User

class FoundPatientAdapter(
    private val patients: List<User>,
    private val onAddClick: (User) -> Unit // Callback pentru adăugarea pacientului
) : RecyclerView.Adapter<FoundPatientAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemFoundPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        holder.bind(patient)
        holder.binding.addButton.setOnClickListener {
            onAddClick(patient) // La click pe "Add", adaugă pacientul
        }
    }

    override fun getItemCount(): Int = patients.size

    inner class PatientViewHolder(val binding: ItemFoundPatientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: User) {
            binding.patientUsername.text = patient.username
            binding.patientName.text = patient.firstName + " " + patient.lastName
            binding.patientEmail.text = patient.email
            binding.patientPhone.text = patient.phoneNumber
//            binding.patientProfilePicture.setImageResource(patient.profilePicture)
        }
    }
}
