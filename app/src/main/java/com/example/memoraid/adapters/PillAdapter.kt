package com.example.memoraid

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.databinding.ItemAppointmentBinding
import com.example.memoraid.databinding.ItemPillBinding
import com.example.memoraid.models.Pill
import com.google.firebase.firestore.FirebaseFirestore

class PillsAdapter(private val pills: MutableList<Pill>) :
    RecyclerView.Adapter<PillsAdapter.PillViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PillViewHolder {
        val itemBinding = ItemPillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PillViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: PillViewHolder, position: Int) {
        val pill = pills[position]
        holder.bind(pill)
    }

    override fun getItemCount(): Int = pills.size

    fun sortPillsByTime() {
        pills.sortWith(compareBy { it.time })
        notifyDataSetChanged()
    }

    inner class PillViewHolder(private val binding: ItemPillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pill: Pill) {
            binding.pillName.text = pill.name
            binding.pillTime.text = pill.time
            binding.pillDose.text = pill.dose
            binding.pillNote.text = pill.note ?: "No notes"
            binding.pillCheckBox.isChecked = pill.isTaken

            updateLayout(pill.isTaken, binding)

            binding.pillCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updatePillStatus(pill, isChecked)
                pill.isTaken = isChecked
                updateLayout(isChecked, binding)
            }
        }

        private fun updatePillStatus(pill: Pill, isTaken: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val pillRef = db.collection("pills").document(pill.id)

            pillRef.update("isTaken", isTaken)
                .addOnSuccessListener {
                    pill.isTaken = isTaken
                }
                .addOnFailureListener {
                }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemPillBinding) {
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

    private fun updateFontStyle(style: Int, binding: ItemPillBinding) {
        binding.pillName.setTypeface(null, style)
        binding.pillDose.setTypeface(null, style)
        binding.pillNote.setTypeface(null, style)
        binding.pillTime.setTypeface(null, style)
    }

    private fun updateFontStyleLabels(style: Int, binding: ItemPillBinding) {
        binding.pillLabelName.setTypeface(null, style)
        binding.pillLabelDose.setTypeface(null, style)
        binding.pillLabelNote.setTypeface(null, style)
        binding.pillLabelTime.setTypeface(null, style)
    }
}
