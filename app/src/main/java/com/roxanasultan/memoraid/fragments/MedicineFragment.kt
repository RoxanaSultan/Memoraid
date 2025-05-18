package com.roxanasultan.memoraid.fragments

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
import com.roxanasultan.memoraid.adapters.MedicineAdapter
import com.roxanasultan.memoraid.databinding.FragmentMedicineBinding
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodel.MedicineViewModel
import com.roxanasultan.memoraid.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicineFragment : Fragment(R.layout.fragment_medicine) {

    private var _binding: FragmentMedicineBinding? = null
    private val binding get() = _binding!!

    private val medicineViewModel: MedicineViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel

    private val medicine = mutableListOf<Medicine>()
    private var medicineAdapter = MedicineAdapter(medicine)

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
        binding.medicineRecyclerView.adapter = medicineAdapter
        binding.medicineRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadUserData() {
        medicineViewModel.loadUser()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicineViewModel.user.collect { user ->
                    user?.let {
                        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                            it.id?.let { userId ->
                                loadMedicine(date, userId)
                                medicineAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadMedicine(date: String, userId: String) {
        medicineViewModel.loadMedicine(date, userId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                medicineViewModel.medicine.collect {  uploadedMedicine->
                    medicine.clear()
                    medicine.addAll(uploadedMedicine)
                    medicineAdapter.sortMedicineByTime()
                    medicineAdapter.notifyDataSetChanged()
                    binding.noMedicineTextView.visibility = if (medicine.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}