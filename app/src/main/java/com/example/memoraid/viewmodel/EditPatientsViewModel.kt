package com.example.memoraid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.repository.UserRepository
import com.example.memoraid.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPatientsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _searchResult = MutableLiveData<User?>()
    val searchResult: LiveData<User?> = _searchResult

    fun loadAssignedPatients() {
        viewModelScope.launch {
            _patients.value = userRepository.getAssignedPatients()
        }
    }

    fun searchPatient(query: String) {
        viewModelScope.launch {
            _searchResult.value = userRepository.findPatientByQuery(query)
        }
    }

    fun addPatientToCaretaker(patient: User) {
        viewModelScope.launch {
            userRepository.addPatientToCurrentCaretaker(patient)
        }
    }

    fun removePatientFromCaretaker(patient: User) {
        userRepository.removePatientFromCaretaker(patient.id) { success ->
            if (success) {
                loadAssignedPatients()
            }
        }
    }
}
