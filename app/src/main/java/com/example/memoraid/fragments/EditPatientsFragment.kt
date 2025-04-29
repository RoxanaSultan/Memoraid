package com.example.memoraid.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.memoraid.adapters.ExistingPatientAdapter
import com.example.memoraid.databinding.FragmentEditPatientsBinding
import com.example.memoraid.viewmodel.EditPatientsViewModel
import com.example.memoraid.models.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class EditPatientsFragment : Fragment() {

    private lateinit var binding: FragmentEditPatientsBinding
    private lateinit var existingPatientAdapter: ExistingPatientAdapter
    private val viewModel: EditPatientsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Creăm adapterul și îi transmitem contextul
        existingPatientAdapter = ExistingPatientAdapter(emptyList(), requireContext()) { patient ->
            viewModel.removePatientFromCaretaker(patient) // Apelezi metoda din ViewModel pentru ștergere
        }

        // Setăm RecyclerView-ul
        binding.patientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.patientsRecyclerView.adapter = existingPatientAdapter

        // Colectăm pacienții din StateFlow
        lifecycleScope.launch {
            viewModel.patients.collect { patients ->
                existingPatientAdapter.updatePatients(patients)
            }
        }

        // Încărcăm pacienții
        viewModel.loadAssignedPatients()
    }
}