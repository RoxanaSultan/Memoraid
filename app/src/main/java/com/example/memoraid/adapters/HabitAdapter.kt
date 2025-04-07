package com.example.memoraid

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.models.Habit
import com.google.firebase.firestore.FirebaseFirestore
import com.example.memoraid.databinding.ItemHabitBinding

class HabitAdapter(private val habits: MutableList<Habit>) :
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
            binding.habitName.text = habit.name
            binding.habitCheckBox.isChecked = habit.isChecked

            updateLayout(habit.isChecked, binding)

            binding.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateHabitStatus(habit, isChecked)
                habit.isChecked = isChecked
                updateLayout(isChecked, binding)
            }
        }

        private fun updateHabitStatus(habit: Habit, isCompleted: Boolean) {
            val db = FirebaseFirestore.getInstance()
            val habitRef = db.collection("habits").document(habit.id)

            habitRef.update("isCompleted", isCompleted)
                .addOnSuccessListener {
                    habit.isChecked = isCompleted
                }
                .addOnFailureListener {}
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
