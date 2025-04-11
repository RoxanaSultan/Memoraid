package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.Medicine
import com.example.memoraid.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository
) : ViewModel() {
    private val _medicine = MutableStateFlow<MutableList<Medicine>>(mutableListOf())
    val medicine: StateFlow<MutableList<Medicine>> get() = _medicine

    fun loadMedicine(date: String) {
        viewModelScope.launch {
            _medicine.value = repository.loadMedicine(date).toMutableList()
        }
    }
}