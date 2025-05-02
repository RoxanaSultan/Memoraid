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
import com.example.memoraid.adapters.MedicineAdapter
import com.example.memoraid.databinding.FragmentMedicineBinding
import com.example.memoraid.models.Medicine
import com.example.memoraid.utils.VerticalSpaceItemDecoration
import com.example.memoraid.viewmodel.MedicineViewModel
import com.example.memoraid.R
import dagger.hilt.android.AndroidEntryPoint

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

        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.medicineRecyclerView.adapter = medicineAdapter

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            loadMedicine(date, medicineViewModel.user.value?.id ?: "")
        }

        binding.medicineRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16)) // 16px spațiu între iteme

        return root
    }

    private fun loadMedicine(date: String, userId: String) {
        medicineViewModel.loadMedicine(date, userId)

        lifecycleScope.launchWhenStarted {
            medicineViewModel.medicine.collect { loadedMedicine ->
                medicine.clear()
                loadedMedicine.forEach { loaded_medicine ->
                    medicine.add(loaded_medicine)
                }

                medicineAdapter = MedicineAdapter(medicine)
                medicineAdapter.sortMedicineByTime()
                binding.medicineRecyclerView.adapter = medicineAdapter
                medicineAdapter.notifyDataSetChanged()

                if (medicine.isEmpty()) {
                    binding.noMedicineTextView.visibility = View.VISIBLE
                } else {
                    binding.noMedicineTextView.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}