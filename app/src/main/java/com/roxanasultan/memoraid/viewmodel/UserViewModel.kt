package com.roxanasultan.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repository.UserRepository
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
}