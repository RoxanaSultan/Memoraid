package com.example.memoraid.fragments

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.adapters.HabitAdapter
import com.example.memoraid.databinding.FragmentHabitsBinding
import com.example.memoraid.models.Habit
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.viewmodel.HabitViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val habitViewModel: HabitViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val habits = mutableListOf<Habit>()
    private var date = ""
    private var habitAdapter = HabitAdapter(habits, date)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        habitViewModel.loadUser()

        binding.habitsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.habitsRecyclerView.adapter = habitAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            loadHabits(date)
        }

        binding.habitsRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))

        return root
    }

    private fun loadHabits(date: String) {
        habitViewModel.loadHabits(habitViewModel.user.value?.id ?: "")

        lifecycleScope.launchWhenStarted {
            habitViewModel.habits.collect { loadedHabits ->
                habits.clear()
                loadedHabits.forEach { habit ->
                    habits.add(habit)
                }

                habitAdapter = HabitAdapter(habits, date)
                binding.habitsRecyclerView.adapter = habitAdapter
                habitAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
