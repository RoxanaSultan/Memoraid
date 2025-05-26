package com.roxanasultan.memoraid.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Patient
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repositories.RegisterRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: RegisterRepository
) : ViewModel() {

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients

    suspend fun isUsernameUnique(username: String): Boolean {
        return repository.isUsernameUnique(username)
    }

    suspend fun isEmailUnique(email: String): Boolean {
        return repository.isEmailUnique(email)
    }

    suspend fun isPhoneNumberUnique(phoneNumber: String): Boolean {
        return repository.isPhoneNumberUnique(phoneNumber)
    }

    fun getPatients() {
        viewModelScope.launch {
            try {
                val list = repository.getPatients()
                _patients.value = list
            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching patients", e)
            }
        }
    }


    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerUser(email, password)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun saveUserDetails(
        uid: String,
        userInfo: HashMap<String, Any?>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.saveUserDetails(uid, userInfo)
            onResult(success)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return repository.getCurrentUser()
    }

    suspend fun getUserById(userId: String): User? {
        return repository.getUserById(userId)
    }

    suspend fun updatePatientCaretakers(patientId: String, updatedCaretakers: MutableList<String>?) {
        try {
            repository.updatePatientCaretakers(patientId, updatedCaretakers)
        } catch (e: Exception) {
            Log.e("ViewModel", "Error updating patient caretakers", e)
        }
    }

    suspend fun updateEmergencyNumbers(patientId: String, updatedCaretakers: MutableList<String>?) {
        try {
            repository.updateEmergencyNumbers(patientId, updatedCaretakers)
        } catch (e: Exception) {
            Log.e("ViewModel", "Error updating patient caretakers", e)
        }
    }
}