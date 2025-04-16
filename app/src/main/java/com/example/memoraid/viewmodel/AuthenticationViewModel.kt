package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.User
import com.example.memoraid.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _authCheckState = MutableStateFlow<Result<User>?>(null)
    val authCheckState: StateFlow<Result<User>?> = _authCheckState

    fun checkIfUserLoggedIn() {
        val currentUser = repository.getCurrentUserId()
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val user = repository.getUserData(currentUser)
                    if (user != null) {
                        _authCheckState.value = Result.success(user)
                    } else {
                        _authCheckState.value = Result.failure(Exception("User not found in Firestore"))
                    }
                } catch (e: Exception) {
                    _authCheckState.value = Result.failure(e)
                }
            }
        } else {
            _authCheckState.value = Result.failure(Exception("User not logged in"))
        }
    }
}
