package com.example.memoraid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memoraid.JournalType
import com.example.memoraid.R
import com.example.memoraid.adapters.JournalAdapter
import com.example.memoraid.adapters.JournalModalAdapter
import com.example.memoraid.databinding.FragmentJournalBinding
import com.example.memoraid.models.Journal
import com.example.memoraid.viewmodel.JournalViewModel
import com.example.memoraid.model.Journal
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JournalsFragment : Fragment() {
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter
    private lateinit var journalModalAdapter: JournalModalAdapter

    private val journalViewModel: JournalViewModel by viewModels()

    private var journals: MutableList<Journal> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupModal()

        journalViewModel.loadJournals()

        lifecycleScope.launchWhenStarted {
            journalViewModel.journals.collect { updatedJournals ->
                journals.clear()
                journals.addAll(updatedJournals)
                journalAdapter.notifyDataSetChanged()
            }
        }

        binding.newJournalImageButton.setOnClickListener {
            binding.modalContainer.visibility = View.VISIBLE
        }

        binding.modalContainer.setOnClickListener {
            binding.modalContainer.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_journalModal_to_journalFragment)
        }
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter(requireContext(), journals,
            onJournalClick = { journal ->
                val bundle = Bundle().apply {
                    putString("journalId", journal.id)
                }
                findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)
            },
            onJournalDelete = { journal ->
                binding.progressContainer.visibility = View.VISIBLE
                journalViewModel.deleteJournal(journal.id) {
                    binding.progressContainer.visibility = View.GONE
                }
            }
        )
        binding.journalRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.journalRecyclerView.adapter = journalAdapter
    }

    private fun setupModal() {
        journalModalAdapter = JournalModalAdapter { position ->
            createJournal(position)
        }
        binding.modalRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = journalModalAdapter
        }
    }

    private fun createJournal(selectedImageIndex: Int) {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date())

        val journalType = when (selectedImageIndex) {
            0 -> JournalType.JOURNAL_PINK.type
            1 -> JournalType.JOURNAL_BLUE.type
            2 -> JournalType.JOURNAL_YELLOW.type
            else -> {
                Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val journalRef = database.collection("journals").document()
        val journalInfo = hashMapOf(
            "userId" to currentUser,
            "entryDate" to formattedDate,
            "title" to "Untitled",
            "text" to "",
            "imageUris" to listOf<String>(),
            "type" to journalType
        )

        journalRef.set(journalInfo).addOnSuccessListener {
            Toast.makeText(requireContext(), "Journal created successfully", Toast.LENGTH_SHORT).show()
            sendNotification()

            val bundle = Bundle().apply {
                putString("journalId", journalRef.id)
            }
            findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)

            binding.modalContainer.visibility = View.GONE
        }.addOnFailureListener { e ->
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to create new journal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    private fun loadJournals() {
        journalList.clear()
        database.collection("journals")
            .whereEqualTo("userId", currentUser)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val journal = document.toObject(Journal::class.java).copy(id = document.id)
                    journalList.add(journal)
            val journalType = when (position) {
                0 -> JournalType.JOURNAL_PINK.type
                1 -> JournalType.JOURNAL_BLUE.type
                else -> {
                    Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                    return@JournalModalAdapter
                }
            }

            journalViewModel.createJournal(journalType, onSuccess = { id ->
                val bundle = Bundle().apply {
                    putString("journalId", id)
                }
                findNavController().navigate(R.id.action_journalFragment_to_journalDetailsFragment, bundle)
                binding.modalContainer.visibility = View.GONE
            }, onFailure = {
                Toast.makeText(requireContext(), "Failed to create journal", Toast.LENGTH_SHORT).show()
            })
        }

        binding.modalRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.modalRecyclerView.adapter = journalModalAdapter
    }
}