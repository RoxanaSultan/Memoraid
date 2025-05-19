package com.roxanasultan.memoraid.patient.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Appointment
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repositories.AppointmentRepository
import com.roxanasultan.memoraid.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _appointments = MutableStateFlow<MutableList<Appointment>>(mutableListOf())
    val appointments: StateFlow<MutableList<Appointment>> get() = _appointments

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getUser()
        }
    }

    fun loadAppointments(date: String, userId: String) {
        viewModelScope.launch {
            _appointments.value = appointmentRepository.loadAppointments(date, userId).toMutableList()
        }
    }
}