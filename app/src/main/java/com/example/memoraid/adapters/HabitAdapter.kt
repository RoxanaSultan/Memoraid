package com.example.memoraid.adapters

import SharedViewModel
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.models.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.example.memoraid.databinding.ItemHabitBinding
import com.example.memoraid.R

class HabitAdapter(private val habits: MutableList<Habit>, internal var date: String) :
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
            binding.habitCheckBox.isChecked = isDateChecked
            updateLayout(isDateChecked, binding)

            binding.habitCheckBox.setOnCheckedChangeListener(null)

            binding.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
                toggleCheckedDate(habit, isChecked, date)
                updateLayout(isChecked, binding)
            }
        }

        private fun toggleCheckedDate(habit: Habit, isChecked: Boolean, today: String) {
            val db = FirebaseFirestore.getInstance()
            val habitRef = db.collection("habits").document(habit.id)

            val updatedDates = ArrayList(habit.checkedDates)

            if (isChecked) {
                if (!updatedDates.contains(today)) {
                    updatedDates.add(today)
                }
            } else {
                updatedDates.remove(today)
            }

            habitRef.update("checkedDates", updatedDates)
                .addOnSuccessListener {
                    habit.checkedDates.clear()
                    habit.checkedDates.addAll(updatedDates)
                }
                .addOnFailureListener {
                    // poți adăuga un toast sau log
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