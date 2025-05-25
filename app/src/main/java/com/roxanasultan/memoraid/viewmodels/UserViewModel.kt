package com.roxanasultan.memoraid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.GeoPoint
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _patient = MutableStateFlow<User?>(null)
    val patient: StateFlow<User?> get() = _patient

    private val _userRole = MutableLiveData<String?>()
    val userRole: LiveData<String?> get() = _userRole

    private val _patientLocation = MutableStateFlow<GeoPoint?>(null)
    val patientLocation: StateFlow<GeoPoint?> get() = _patientLocation

    fun fetchUserRole() {
        viewModelScope.launch {
            val role = userRepository.getUserRole()
            _userRole.value = role
        }
    }

    fun loadUser() {
        val userId = userRepository.getCurrentUser()?.uid
        viewModelScope.launch {
            _user.value = userRepository.getUser(userId!!)
        }
    }

    suspend fun getUser(userId: String): User? {
        return userRepository.getUser(userId)
    }

    fun loadPatient() {
        val userId = userRepository.getCurrentUser()?.uid
        viewModelScope.launch {
            _patient.value = userRepository.getPatient(userId!!)
        }
    }

    fun observePatientLocation(patientId: String) {
        viewModelScope.launch {
            userRepository.observePatientLocation(patientId).collect {
                _patientLocation.value = it
            }
        }
    }
}