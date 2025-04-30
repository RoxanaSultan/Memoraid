package com.example.memoraid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.adapters.ExistingPatientAdapter
import com.example.memoraid.adapters.FoundPatientAdapter
import com.example.memoraid.databinding.FragmentEditPatientsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.memoraid.viewmodel.AccountCaretakerViewModel

@AndroidEntryPoint
class EditPatientsFragment : Fragment() {

    private lateinit var binding: FragmentEditPatientsBinding
    private lateinit var existingPatientAdapter: ExistingPatientAdapter
    private lateinit var foundPatientAdapter: FoundPatientAdapter
    private val viewModel: AccountCaretakerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupRecyclerViews()
        setupObservers()
        setupSearch()

        viewModel.loadAssignedPatients()
    }

    private fun setupAdapters() {
        existingPatientAdapter = ExistingPatientAdapter(
            patients = emptyList(),
            context = requireContext(),
            onDeleteClick = { patient ->
                viewModel.removePatientFromCaretaker(patient)
            }
        )

        foundPatientAdapter = FoundPatientAdapter(
            patients = emptyList(),
            context = requireContext(),
            onAddClick = { patient ->
                viewModel.addPatientToCaretaker(patient)
                // Ascunde RecyclerView-ul după adăugare
                binding.foundPatientsRecyclerView.visibility = View.GONE
            }
        )
    }

    private fun setupRecyclerViews() {
        binding.patientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = existingPatientAdapter
        }

        binding.foundPatientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foundPatientAdapter
            visibility = View.GONE // Inițial ascuns
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            // Observer pentru pacienții asignați
            viewModel.patients.collect { patients ->
                existingPatientAdapter.updatePatients(patients)
            }
        }

        lifecycleScope.launchWhenStarted {
            // Observer pentru rezultatele căutării
            viewModel.searchResults.collect { results ->
                if (results.isNotEmpty()) {
                    foundPatientAdapter.updatePatients(results)
                    binding.foundPatientsRecyclerView.visibility = View.VISIBLE
                } else {
                    binding.foundPatientsRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim() ?: ""
            if (query.length >= 2) { // Caută doar dacă sunt cel puțin 2 caractere
                viewModel.searchPatients(query)
            } else {
                // Ascunde rezultatele dacă textul este prea scurt sau gol
                binding.foundPatientsRecyclerView.visibility = View.GONE
                viewModel.clearSearchResults()
            }
        }
    }
}