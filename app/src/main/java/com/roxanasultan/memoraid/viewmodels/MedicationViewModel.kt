package com.roxanasultan.memoraid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
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

    private val _allMedication = MutableStateFlow<MutableList<Medicine>>(mutableListOf())
    val allMedication: StateFlow<MutableList<Medicine>> get() = _allMedication

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private var snapshotListener: ListenerRegistration? = null

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getUser()
        }
    }

    fun loadMedicine(date: String, userId: String) {
        snapshotListener?.remove()

        snapshotListener = medicationRepository.observeMedication(date, userId) { updatedMedication ->
            _medicine.value = updatedMedication.toMutableList()
        }
    }

    fun loadAllMedicationForUser(userId: String) {
        viewModelScope.launch {
            _allMedication.value = medicationRepository.loadAllMedicationForUser(userId).toMutableList()
        }
    }

    fun setAlarm(medicationId: String, hasAlarm: Boolean) {
        viewModelScope.launch {
            medicationRepository.setAlarm(medicationId, hasAlarm)
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