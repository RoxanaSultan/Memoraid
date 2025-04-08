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
import com.example.memoraid.adapters.JournalAdapter
import com.example.memoraid.adapters.JournalModalAdapter
import com.example.memoraid.databinding.FragmentJournalBinding
import com.example.memoraid.viewmodel.JournalViewModel
import com.example.memoraid.models.Journal
import com.example.memoraid.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JournalsFragment : Fragment(R.layout.fragment_journal) {
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
            val journalType = when (position) {
                0 -> JournalType.JOURNAL_PINK.type
                1 -> JournalType.JOURNAL_BLUE.type
                2 -> JournalType.JOURNAL_YELLOW.type
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