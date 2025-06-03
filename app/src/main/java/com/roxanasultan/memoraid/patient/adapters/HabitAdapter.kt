package com.roxanasultan.memoraid.patient.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.models.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.roxanasultan.memoraid.databinding.ItemHabitBinding
import com.roxanasultan.memoraid.R

class HabitAdapter(
    private val habits: MutableList<Habit>,
    internal var date: String,
    private val onCheckClick: (Habit) -> Unit
) :
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val itemBinding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.bind(habit)
    }

    override fun getItemCount(): Int = habits.size

    inner class HabitViewHolder(private val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit) {
            val isDateChecked = habit.checkedDates.contains(date)

            binding.habitName.text = habit.name

            binding.habitCheckBox.setOnCheckedChangeListener(null)
            binding.habitCheckBox.isChecked = isDateChecked
            updateLayout(isDateChecked, binding)

            binding.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != habit.checkedDates.contains(date)) {
                    onCheckClick(habit)
                }
                updateLayout(isChecked, binding)
            }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemHabitBinding) {
        if (completed) {
            binding.root.alpha = 0.5f
            binding.root.background = binding.root.context.getDrawable(R.drawable.completed_background)
            updateFontStyle(Typeface.ITALIC, binding)
        } else {
            binding.root.alpha = 1f
            binding.root.background = binding.root.context.getDrawable(R.drawable.layout_background)
            updateFontStyle(Typeface.NORMAL, binding)
        }
    }

    private fun updateFontStyle(style: Int, binding: ItemHabitBinding) {
        binding.habitName.setTypeface(null, style)
    }
}