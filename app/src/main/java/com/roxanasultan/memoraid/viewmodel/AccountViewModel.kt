package com.roxanasultan.memoraid.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.repository.UserRepository
import com.roxanasultan.memoraid.models.User
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _patient = MutableStateFlow<User?>(null)
    val patient: StateFlow<User?> get() = _patient

    private var snapshotListener: ListenerRegistration? = null

    init {
        setupUserListener()
    }

    private fun setupUserListener() {
        val userId = userRepository.getCurrentUser()?.uid ?: return
        snapshotListener?.remove()

        snapshotListener = userRepository.observeUser(userId) { user ->
            _user.value = user
        }
    }

    fun loadUser() {
        val userId = userRepository.getCurrentUser()?.uid
        viewModelScope.launch {
            _user.value = userRepository.getUser(userId!!)
        }
    }

    fun logout() {
        userRepository.logout()
    }

    fun uploadAndSaveProfilePicture(uri: Uri, userId: String) {
        viewModelScope.launch {
            userRepository.uploadAndSaveProfilePicture(uri, userId)
        }
    }

    fun removeProfilePicture(userId: String) {
        viewModelScope.launch {
            userRepository.removeProfilePicture(userId)
        }
    }

    fun deleteImageFromStorage(image: String) {
        viewModelScope.launch {
            userRepository.deleteImageFromStorage(image)
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        val userId = user.value?.id ?: return
        userRepository.updateUserLocation(userId, latitude, longitude)
    }

    override fun onCleared() {
        snapshotListener?.remove()
        super.onCleared()
    }
}