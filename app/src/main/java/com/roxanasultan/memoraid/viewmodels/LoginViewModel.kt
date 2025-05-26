package com.roxanasultan.memoraid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.repositories.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: LoginRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<Unit>?>(null)
    val loginState: StateFlow<Result<Unit>?> = _loginState

    fun login(credential: String, password: String) {
        viewModelScope.launch {
            try {
                val email = when {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(credential).matches() -> {
                        // Credential este email deja
                        credential
                    }
                    credential.all { it.isDigit() } && credential.length in 7..15 -> {
                        // E numar de telefon (simplu check)
                        repository.getEmailByPhoneNumber(credential)
                    }
                    else -> {
                        // Presupunem username
                        repository.getEmailByUsername(credential)
                    }
                }

                if (email == null) {
                    _loginState.value = Result.failure(Exception("User not found"))
                    return@launch
                }

                val loginResult = repository.loginWithEmail(email, password)
                _loginState.value = loginResult

            } catch (e: Exception) {
                _loginState.value = Result.failure(e)
            }
        }
    }

    fun sendResetEmail(username: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val email = repository.getEmailByUsername(username)
                if (email == null) {
                    onResult(Result.failure(Exception("Username not found")))
                    return@launch
                }

                val result = repository.sendPasswordResetEmail(email)
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    suspend fun doesProfileExist(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val exists = repository.doesProfileExist(email)
                onResult(exists)
            } catch (e: Exception) {
                onResult(false)  // în caz de eroare, presupunem că nu există
            }
        }
    }

    fun clearState() {
        _loginState.value = null
    }
}
