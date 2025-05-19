package com.roxanasultan.memoraid.caretaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.caretaker.adapters.ExistingPatientAdapter
import com.roxanasultan.memoraid.caretaker.adapters.FoundPatientAdapter
import com.roxanasultan.memoraid.databinding.FragmentEditPatientsBinding
import com.roxanasultan.memoraid.viewmodels.AccountCaretakerViewModel
import dagger.hilt.android.AndroidEntryPoint

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
            context = requireContext()
        ) { patient ->
            viewModel.removePatientFromCaretaker(patient)
        }

        foundPatientAdapter = FoundPatientAdapter(
            patients = emptyList(),
            context = requireContext()
        ) { patient ->
            // Adaugă pacientul și resetează interfața
            viewModel.addPatientToCaretaker(patient)
            binding.searchEditText.text?.clear()
            binding.foundPatientsRecyclerView.visibility = View.GONE
        }
    }

    private fun setupRecyclerViews() {
        binding.patientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = existingPatientAdapter
            setHasFixedSize(true)
        }

        binding.foundPatientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foundPatientAdapter
            visibility = View.GONE
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            // Observer pentru pacienții asignați
            viewModel.patients.collect { patients ->
                existingPatientAdapter.updatePatients(patients)
                // Forțează refresh UI
                binding.patientsRecyclerView.post {
                    existingPatientAdapter.notifyDataSetChanged()
                }
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
        binding.searchEditText.apply {
            doOnTextChanged { text, _, _, _ ->
                val query = text?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    viewModel.searchPatients(query)
                } else {
                    binding.foundPatientsRecyclerView.visibility = View.GONE
                }
            }

            // Opțional: Focus automat pe câmpul de căutare
            requestFocus()
        }
    }
}