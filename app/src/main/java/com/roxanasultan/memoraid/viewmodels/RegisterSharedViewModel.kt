package com.roxanasultan.memoraid.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegisterSharedViewModel : ViewModel() {
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> get() = _password

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _firstName = MutableLiveData<String>()
    val firstName: LiveData<String> get() = _firstName

    private val _lastName = MutableLiveData<String>()
    val lastName: LiveData<String> get() = _lastName

    private val _patientsList = MutableLiveData<List<String>>()
    val patientsList: LiveData<List<String>> get() = _patientsList

    private val _role = MutableLiveData<String>()
    val role: LiveData<String> get() = _role

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> get() = _phoneNumber

    private val _birthdate = MutableLiveData<String>()
    val birthdate: LiveData<String> get() = _birthdate

    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    private val _selectedPatient = MutableLiveData<String?>()
    val selectedPatient: LiveData<String?> get() = _selectedPatient

    fun setPassword(password: String) {
        _password.value = password
    }

    fun setRole(role: String) {
        _role.value = role
    }

    fun setFirstName(value: String) {
        _firstName.value = value
    }

    fun setLastName(value: String) {
        _lastName.value = value
    }

    fun setUsername(value: String) {
        _username.value = value
    }

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPatientsList(patients: List<String>) {
        _patientsList.value = patients
    }

    fun setPhoneNumber(value: String) {
        _phoneNumber.value = value
    }

    fun setBirthdate(value: String) {
        _birthdate.value = value
    }

    fun setProfilePicture(value: String?) {
        _profilePictureUrl.value = value
    }

    fun setSelectedPatient(patientId: String?) {
        _selectedPatient.value = patientId
    }

    fun clearData() {
        _username.value = ""
        _password.value = ""
        _email.value = ""
        _firstName.value = ""
        _lastName.value = ""
        _patientsList.value = emptyList()
        _role.value = ""
        _phoneNumber.value = ""
        _birthdate.value = ""
        _profilePictureUrl.value = ""
        _selectedPatient.value = null
    }
}
