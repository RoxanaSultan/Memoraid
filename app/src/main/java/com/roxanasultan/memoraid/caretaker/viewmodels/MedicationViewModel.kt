package com.roxanasultan.memoraid.caretaker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Medicine
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repositories.MedicationRepository
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
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
            _medicine.value = medicationRepository.loadMedicine(date, userId).toMutableList()
        }
    }

    fun addMedicine(medicine: Medicine, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val id = medicationRepository.addMedicine(medicine)
            if (id != null) onSuccess(id) else onFailure()
        }
    }

    fun updateMedicine(medicine: Medicine, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = medicationRepository.updateMedicine(medicine)
            if (success) onSuccess() else onFailure()
        }
    }

    fun deleteMedicine(medicineId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = medicationRepository.deleteMedicine(medicineId)
            onComplete(success)
        }
    }
}