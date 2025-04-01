package com.example.memoraid

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.databinding.FragmentHabitsBinding
import com.example.memoraid.models.Habit
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String
    private lateinit var sharedViewModel: SharedViewModel

    private val habitList = mutableListOf<Habit>()
    private val habitAdapter = HabitAdapter(habitList)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        database = FirebaseFirestore.getInstance()
        authenticator = FirebaseAuth.getInstance()
        currentUser = authenticator.currentUser?.uid ?: ""

        binding.habitsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.habitsRecyclerView.adapter = habitAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            getHabits(date)
        }

        binding.habitsRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16)) // 16px spacing

        return root
    }

    private fun getHabits(date: String) {
        val habits = database.collection("habits")
            .whereEqualTo("userId", currentUser)
            .get()

        habits.addOnSuccessListener { documents ->
            habitList.clear()
            for (document in documents) {
                val habit = document.toObject(Habit::class.java)
                habit.id = document.id
                habit.isChecked = document.get("isChecked") as? Boolean ?: false
                habitList.add(habit)
            }
            habitAdapter.notifyDataSetChanged()
        }
        habits.addOnFailureListener { exception ->
            Toast.makeText(context, "Error getting habits: $exception", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
