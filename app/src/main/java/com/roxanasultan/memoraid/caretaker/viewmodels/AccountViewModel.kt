package com.roxanasultan.memoraid.caretaker.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.repositories.UserRepository
import com.roxanasultan.memoraid.models.User
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _selectedPatient = MutableStateFlow<User?>(null)
    val selectedPatient: StateFlow<User?> get() = _selectedPatient

    private val _availablePatients = MutableStateFlow<List<User?>>(emptyList())
    val availablePatients: StateFlow<List<User?>> get() = _availablePatients

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> get() = _searchResults

    private val _patients = MutableStateFlow<List<User?>>(emptyList())
    val patients: StateFlow<List<User?>> get() = _patients

    private var snapshotListener: ListenerRegistration? = null

    init {
        setupUserListener()
    }

    private fun setupUserListener() {
        val userId = repository.getCurrentUser()?.uid ?: return
        snapshotListener?.remove()

        snapshotListener = repository.observeUser(userId) { user ->
            _user.value = user
        }
    }

    fun getOtherPatients() {
        viewModelScope.launch {
            _availablePatients.value = repository.getOtherPatients()
        }
    }

    fun logout() {
        repository.logout()
    }

    fun loadPatient() {
        val userId = repository.getCurrentUser()?.uid
        viewModelScope.launch {
            _selectedPatient.value = repository.getPatient(userId!!)
        }
    }

    fun loadUser() {
        val userId = repository.getCurrentUser()?.uid
        viewModelScope.launch {
            _user.value = repository.getUser(userId!!)
        }
    }

    fun uploadAndSaveProfilePicture(uri: Uri, userId: String) {
        viewModelScope.launch {
            repository.uploadAndSaveProfilePicture(uri, userId)
        }
    }

    fun removeProfilePicture(userId: String) {
        viewModelScope.launch {
            repository.removeProfilePicture(userId)
        }
    }

    suspend fun saveUserDetails(userUpdates: Map<String, String>, userId: String): Boolean {
        return repository.saveUserDetails(userUpdates.toMutableMap(), userId)
    }

    suspend fun isUsernameUnique(username: String): Boolean {
        return repository.isUsernameUnique(username)
    }

    suspend fun isEmailUnique(email: String): Boolean {
        return repository.isEmailUnique(email)
    }

    fun deleteImageFromStorage(image: String) {
        viewModelScope.launch {
            repository.deleteImageFromStorage(image)
        }
    }


    fun selectPatient(patientId: String) {
        viewModelScope.launch {
            _selectedPatient.value = repository.selectPatient(patientId)
        }
    }

    fun loadAssignedPatients() {
        viewModelScope.launch {
            _patients.value = repository.getAssignedPatients()
        }
    }

    fun searchPatients(query: String) {
        viewModelScope.launch {
            if (query.isNotEmpty()) {
                val results = repository.findPatientsByQuery(query)
                val currentPatients = _patients.value.map { it?.id }
                _searchResults.value = results.filterNot { patient ->
                    currentPatients.contains(patient.id)
                }
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun addPatientToCaretaker(patient: User) {
        viewModelScope.launch {
            repository.addPatientToCurrentCaretaker(patient)
            loadAssignedPatients()
            _searchResults.value = searchResults.value.filterNot { it.id == patient.id }
        }
    }

    fun removePatientFromCaretaker(patient: User) {
        repository.removePatientFromCaretaker(patient.id) { success ->
            if (success) {
                loadAssignedPatients()
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    override fun onCleared() {
        snapshotListener?.remove()
        super.onCleared()
    }
}