package com.roxanasultan.memoraid.caretaker.fragments

import SharedViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.caretaker.adapters.HabitAdapter
import com.roxanasultan.memoraid.databinding.FragmentHabitsCaretakerBinding
import com.roxanasultan.memoraid.models.Habit
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodels.HabitViewModel
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsCaretakerBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val habits = mutableListOf<Habit>()

    private lateinit var habitAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsCaretakerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val date = sharedViewModel.selectedDate.value ?: return root

        habitAdapter = HabitAdapter(
            habits,
            date,
            onEditClick = { habit -> showAddHabitDialog(habit) },
            onDeleteClick = { habit -> showDeleteConfirmationDialog(habit) },
            onCheckClick = { habit -> toggleCheckedDate(habit, !habit.checkedDates.contains(sharedViewModel.selectedDate.value!!), sharedViewModel.selectedDate.value!!) }
        )

        loadUserData()

        setupRecyclerView()

        binding.addHabitButton.setOnClickListener {
            showAddHabitDialog()
        }

        return root
    }

    private fun toggleCheckedDate(habit: Habit, isChecked: Boolean, today: String) {
        val updatedDates = habit.checkedDates.toMutableList()

        if (isChecked) {
            if (!updatedDates.contains(today)) {
                updatedDates.add(today)
            }
        } else {
            updatedDates.remove(today)
        }

        habitViewModel.updateHabitCheckedDates(habit.id!!, updatedDates) { success ->
            if (success) {
                habit.checkedDates.clear()
                habit.checkedDates.addAll(updatedDates)
            } else {
                Toast.makeText(requireContext(), "Error updating habit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddHabitDialog(habit: Habit? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_habit, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (habit == null) "Add New Habit" else "Edit Habit")
            .setView(dialogView)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        habit?.let {
            etName.setText(it.name)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()

            if (validateHabitInput(name)) {
                val newHabit = habit?.copy(
                    id = habit.id,
                    name = name,
                    userId = habitViewModel.user.value?.selectedPatient ?: "",
                    checkedDates = habit.checkedDates
                ) ?: Habit(
                    name = name,
                    userId = habitViewModel.user.value?.selectedPatient ?: "",
                    checkedDates = arrayListOf())

                saveHabit(newHabit)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        binding.habitsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.habitsRecyclerView.adapter = habitAdapter
        binding.habitsRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        habitViewModel.loadUser()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                habitViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.selectedPatient?.let { patientId ->
                                loadHabits(patientId)
                                habitAdapter.date = date
                                habitAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadHabits(userId: String) {
        habitViewModel.loadHabits(userId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                habitViewModel.habits.collect { loadedHabits ->
                    habits.clear()
                    habits.addAll(loadedHabits)
                    habitAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun validateHabitInput(name: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter habit name", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showDeleteConfirmationDialog(habit: Habit) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this habit?")
            .setPositiveButton("Yes") { _, _ -> deleteHabit(habit) }
            .setNegativeButton("No") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun deleteHabit(habit: Habit) {
        lifecycleScope.launch {
            habitViewModel.deleteHabit(habit.id) { result ->
                if (result) {
                    Toast.makeText(requireContext(), "Habit deleted successfully", Toast.LENGTH_SHORT).show()
                    loadHabits(habitViewModel.user.value?.selectedPatient ?: "")
                } else {
                    Toast.makeText(requireContext(), "Error deleting habit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveHabit(habit: Habit) {
        lifecycleScope.launch {
            if (habit.id != null && habit.id.isNotEmpty()) {
                habitViewModel.updateHabit(
                    habit,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Habit updated successfully", Toast.LENGTH_SHORT).show()
                        loadHabits(habitViewModel.user.value?.selectedPatient ?: "")
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error updating habit", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                habitViewModel.addHabit(
                    habit,
                    onSuccess = { id ->
                        Toast.makeText(requireContext(), "Habit added successfully", Toast.LENGTH_SHORT).show()
                        loadHabits(habitViewModel.user.value?.selectedPatient ?: "")
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Error saving habit", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
