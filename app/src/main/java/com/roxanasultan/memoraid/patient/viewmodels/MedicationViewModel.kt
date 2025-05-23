package com.roxanasultan.memoraid.patient.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repositories.MedicineRepository
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _medicine = MutableStateFlow<MutableList<Medicine>>(mutableListOf())
    val medicine: StateFlow<MutableList<Medicine>> get() = _medicine

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getUser()
        }
    }

    fun loadMedicine(date: String, userId: String) {
        viewModelScope.launch {
            _medicine.value = medicineRepository.loadMedicine(date, userId).toMutableList()
        }
    }
}