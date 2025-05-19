package com.roxanasultan.memoraid.viewmodels

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
class AppointmentViewModel @Inject constructor(
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

    fun createAppointment(appointment: Appointment, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val id = appointmentRepository.createAppointment(appointment)
            if (id != null) onSuccess(id) else onFailure()
        }
    }

    fun updateAppointment(appointment: Appointment, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = appointmentRepository.updateAppointment(appointment)
            if (success) onSuccess() else onFailure()
        }
    }

    fun deleteAppointment(appointmentId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = appointmentRepository.deleteAppointment(appointmentId)
            onComplete(success)
        }
    }
}