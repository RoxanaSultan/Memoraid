package com.example.memoraid.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.repository.UserRepository
import com.example.memoraid.models.User
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountPatientViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _patient = MutableStateFlow<User?>(null)
    val patient: StateFlow<User?> get() = _patient

    private var snapshotListener: ListenerRegistration? = null

//    init {
//        setupUserListener()
//    }

//    private fun setupUserListener() {
//        val userId = repository.getCurrentUser()?.uid ?: return
//        snapshotListener?.remove()
//
//        snapshotListener = repository.observeUser(userId) { user ->
//            _user.value = user
//        }
//    }

    fun loadPatient() {
        val userId = repository.getCurrentUser()?.uid
        viewModelScope.launch {
            _patient.value = repository.getPatient(userId!!)
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

    override fun onCleared() {
        snapshotListener?.remove()
        super.onCleared()
    }
}