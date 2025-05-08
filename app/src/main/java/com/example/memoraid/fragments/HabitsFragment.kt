package com.example.memoraid.fragments

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
import com.example.memoraid.R
import com.example.memoraid.adapters.HabitAdapter
import com.example.memoraid.databinding.FragmentHabitsBinding
import com.example.memoraid.models.Habit
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val habits = mutableListOf<Habit>()

    private lateinit var habitAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val date = sharedViewModel.selectedDate.value ?: return root

        habitAdapter = HabitAdapter(
            habits,
            date,
            onCheckClick = { habit -> toggleCheckedDate(habit, !habit.checkedDates.contains(sharedViewModel.selectedDate.value!!), sharedViewModel.selectedDate.value!!) }
        )

        loadUserData()
        setupRecyclerView()

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
                            it.id?.let { userId ->
                                loadHabits(userId)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
