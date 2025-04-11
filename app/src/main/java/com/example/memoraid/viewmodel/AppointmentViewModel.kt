package com.example.memoraid.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.Appointment
import com.example.memoraid.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val repository: AppointmentRepository
) : ViewModel() {
    private val _appointments = MutableStateFlow<MutableList<Appointment>>(mutableListOf())
    val appointments: StateFlow<MutableList<Appointment>> get() = _appointments

//    private val _appointmentDetails = MutableStateFlow<Appointment?>(null)
//    val appointmentDetails: StateFlow<Appointment?> get() = _appointmentDetails
//
//    private val _isSaving = MutableStateFlow(false)
//    val isSaving: StateFlow<Boolean> get() = _isSaving

    fun loadAppointments(date: String) {
        viewModelScope.launch {
            _appointments.value = repository.loadAppointments(date).toMutableList()
        }
    }

//    fun createAppointment(appointment: Appointment, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
//        viewModelScope.launch {
//            val id = repository.createAppointment(appointment)
//            if (id != null) onSuccess(id) else onFailure()
//        }
//    }
//
//    fun deleteAppointment(appointmentId: String, onComplete: () -> Unit) {
//        viewModelScope.launch {
//            val result = repository.deleteAppointment(appointmentId)
//            if (result) {
//                loadAppointments(appointmentId)
//            }
//            onComplete()
//        }
//    }
//
//    fun loadAppointmentDetails(appointmentId: String) {
//        viewModelScope.launch {
//            _appointmentDetails.value = repository.loadAppointmentDetails(appointmentId)
//        }
//    }
//
//    suspend fun saveAppointmentDetails(appointment: Appointment): Boolean {
//        _isSaving.value = true
//        return try {
//            val success = repository.saveAppointmentDetails(appointment)
//            success
//        } finally {
//            _isSaving.value = false
//        }
//    }
}