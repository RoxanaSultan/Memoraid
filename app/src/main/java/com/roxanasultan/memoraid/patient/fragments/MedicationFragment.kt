package com.roxanasultan.memoraid.patient.fragments

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.patient.adapters.MedicineAdapter
import com.roxanasultan.memoraid.databinding.FragmentMedicineBinding
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodels.MedicationViewModel
import com.roxanasultan.memoraid.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationFragment : Fragment(R.layout.fragment_medicine) {

    private var _binding: FragmentMedicineBinding? = null
    private val binding get() = _binding!!

    private val medicationViewModel: MedicationViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val medication = mutableListOf<Medicine>()
    private var medicationAdapter = MedicineAdapter(medication)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        setupRecyclerView()
        loadUserData()

        return root
    }

    private fun setupRecyclerView() {
        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.medicineRecyclerView.adapter = medicationAdapter
        binding.medicineRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        medicationViewModel.loadUser()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicationViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.id?.let { userId ->
                                loadMedicine(date, userId)
                                medicationAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadMedicine(date: String, userId: String) {
        medicationViewModel.loadMedicine(date, userId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicationViewModel.medicine.collect {  uploadedMedicine->
                    medication.clear()
                    medication.addAll(uploadedMedicine)
                    medicationAdapter.sortMedicineByTime()
                    medicationAdapter.notifyDataSetChanged()
                    binding.noMedicineTextView.visibility = if (medication.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}