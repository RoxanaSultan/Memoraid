package com.roxanasultan.memoraid.patient.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.databinding.ItemMedicineBinding
import com.roxanasultan.memoraid.models.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.R

class MedicineAdapter(private val medicine: MutableList<Medicine>) :
    RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val itemBinding = ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicineViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicine[position]
        holder.bind(medicine)
    }

    override fun getItemCount(): Int = medicine.size

    fun sortMedicineByTime() {
        medicine.sortWith(compareBy { it.time })
        notifyDataSetChanged()
    }

    inner class MedicineViewHolder(private val binding: ItemMedicineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            binding.medicineName.text = medicine.name
            binding.medicineTime.text = medicine.time
            binding.medicineDose.text = medicine.dose
            binding.medicineNote.text = medicine.note ?: "No notes"
            binding.medicineCheckBox.isChecked = medicine.taken

            updateLayout(medicine.taken, binding)

            binding.medicineCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateMedicineStatus(medicine, isChecked)
                medicine.taken = isChecked
                updateLayout(isChecked, binding)
            }
        }

        private fun updateMedicineStatus(medicine: Medicine, taken: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val medicineRef = db.collection("medicine").document(medicine.id)

            medicineRef.update("taken", taken)
                .addOnSuccessListener {
                    medicine.taken = taken
                }
                .addOnFailureListener {
                }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemMedicineBinding) {
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

    private fun updateFontStyle(style: Int, binding: ItemMedicineBinding) {
        binding.medicineName.setTypeface(null, style)
        binding.medicineDose.setTypeface(null, style)
        binding.medicineNote.setTypeface(null, style)
        binding.medicineTime.setTypeface(null, style)
        binding.medicineCheckBox.setTypeface(null, style)
    }

    private fun updateFontStyleLabels(style: Int, binding: ItemMedicineBinding) {
        binding.medicineLabelName.setTypeface(null, style)
        binding.medicineLabelDose.setTypeface(null, style)
        binding.medicineLabelNote.setTypeface(null, style)
        binding.medicineLabelTime.setTypeface(null, style)
        binding.medicineLabelCheckBox.setTypeface(null, style)
    }
}
