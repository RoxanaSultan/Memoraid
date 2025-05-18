package com.roxanasultan.memoraid.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.models.Habit
import com.roxanasultan.memoraid.databinding.ItemHabitCaretakerBinding
import com.roxanasultan.memoraid.R

class HabitCaretakerAdapter(
    private val habits: MutableList<Habit>,
    internal var date: String,
    private val onEditClick: (Habit) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onCheckClick: (Habit) -> Unit
    ) :
    RecyclerView.Adapter<HabitCaretakerAdapter.HabitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val itemBinding =
            ItemHabitCaretakerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.bind(habit)
    }

    override fun getItemCount(): Int = habits.size

    inner class HabitViewHolder(private val binding: ItemHabitCaretakerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit) {
            val isDateChecked = habit.checkedDates.contains(date)

            binding.habitName.text = habit.name

            binding.habitCheckBox.setOnCheckedChangeListener(null)
            binding.habitCheckBox.isChecked = isDateChecked

            binding.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckClick(habit)
                updateLayout(isChecked, binding)
            }

            updateLayout(isDateChecked, binding)

            binding.editButton.setOnClickListener {
                onEditClick(habit)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(habit)
            }
        }
    }

    private fun updateLayout(completed: Boolean, binding: ItemHabitCaretakerBinding) {
        if (completed) {
            binding.root.alpha = 0.5f
            binding.root.background =
                binding.root.context.getDrawable(R.drawable.completed_background)
            updateFontStyle(Typeface.ITALIC, binding)
        } else {
            binding.root.alpha = 1f
            binding.root.background =
                binding.root.context.getDrawable(R.drawable.layout_background)
            updateFontStyle(Typeface.NORMAL, binding)
        }
    }

    private fun updateFontStyle(style: Int, binding: ItemHabitCaretakerBinding) {
        binding.habitName.setTypeface(null, style)
    }
}