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
import com.example.memoraid.databinding.FragmentPillsBinding
import com.example.memoraid.models.Appointment
import com.example.memoraid.models.Pill
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PillsFragment : Fragment() {

    private var _binding: FragmentPillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String
    private lateinit var sharedViewModel: SharedViewModel

    private val pillList = mutableListOf<Pill>()
    private val pillAdapter = PillsAdapter(pillList)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPillsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        database = FirebaseFirestore.getInstance()
        authenticator = FirebaseAuth.getInstance()
        currentUser = authenticator.currentUser?.uid ?: ""

        binding.pillsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pillsRecyclerView.adapter = pillAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            getPills(date)
        }

        binding.pillsRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16)) // 16px spațiu între iteme

        return root
    }

    private fun getPills(date: String) {
        val pills = database.collection("pills")
            .whereEqualTo("userId", currentUser)
            .whereEqualTo("date", date)
            .get()

        pills.addOnSuccessListener { documents ->
            pillList.clear()
            for (document in documents) {
                val pill = document.toObject(Pill::class.java)
                pill.id = document.id
                pill.isTaken = document.get("isTaken") as? Boolean ?: false
                pillList.add(pill)
            }
            pillAdapter.sortPillsByTime()
            pillAdapter.notifyDataSetChanged()

            if (pillList.isEmpty()) {
                binding.noPillsTextView.visibility = View.VISIBLE
            } else {
                binding.noPillsTextView.visibility = View.GONE
            }
        }
        pills.addOnFailureListener { exception ->
            Toast.makeText(context, "Error getting pills: $exception", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}