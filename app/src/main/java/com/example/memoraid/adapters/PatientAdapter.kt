package com.example.memoraid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.databinding.ItemPatientBinding
import com.example.memoraid.models.PatientModel
import com.squareup.picasso.Picasso

class PatientAdapter(
    private val context: Context,
    private val patientsList: List<PatientModel>,
    private val onCheckboxChanged: (PatientModel, Boolean) -> Unit // Callback to pass checkbox state
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(LayoutInflater.from(context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patientsList[position]
        holder.bind(patient)
    }

    override fun getItemCount(): Int {
        return patientsList.size
    }

    inner class PatientViewHolder(private val binding: ItemPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: PatientModel) {
            Picasso.get()
                .load(patient.profilePicture) // Assuming `profilePicture` is the URL
                .into(binding.patientProfilePicture) // Load image into the ImageView

            binding.patientUsername.text = patient.username
            binding.patientName.text = patient.name
            binding.checkboxPatientSelect.setOnCheckedChangeListener { _, isChecked ->
                onCheckboxChanged(patient, isChecked)
            }
        }

    }
}

