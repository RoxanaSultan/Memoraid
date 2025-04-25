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
class AccountViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

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

    fun loadUser() {
        val userId = repository.getCurrentUser()?.uid
        viewModelScope.launch {
            _user.value = repository.getUser(userId!!)
        }
    }

    fun uploadAndSaveProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.uploadAndSaveProfilePicture(uri)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeProfilePicture() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.removeProfilePicture()
            } finally {
                _isLoading.value = false
            }
        }
    }

//
//    fun uploadImageToStorage(uri: Uri) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                repository.uploadImageToStorage(uri)
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    suspend fun saveUserDetails(userUpdates: Map<String, String>): Boolean {
        _isLoading.value = true
        return try {
            repository.saveUserDetails(userUpdates.toMutableMap())
        } finally {
            _isLoading.value = false
        }
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