package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _passwordChangeResult = MutableStateFlow<Result<Unit>?>(null)
    val passwordChangeResult: StateFlow<Result<Unit>?> = _passwordChangeResult

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _passwordChangeResult.value = repository.updatePassword(newPassword)
        }
    }

    fun clearState() {
        _passwordChangeResult.value = null
    }
}