package com.roxanasultan.memoraid.patient.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _lastRouteLocation = MutableStateFlow<GeoPoint?>(null)
    val lastRouteLocation: StateFlow<GeoPoint?> = _lastRouteLocation

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
            Log.d("AccountViewModel", "Loading user with ID: $userId")
            _user.value = userRepository.getUser(userId!!)
        }
    }

    fun logout() {
        userRepository.logout()
    }

    fun fetchLastRouteLocation(userId: String) {
        userRepository.getLastRouteLocation(userId) { location ->
            _lastRouteLocation.value = location
        }
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

    override fun onCleared() {
        snapshotListener?.remove()
        super.onCleared()
    }
}