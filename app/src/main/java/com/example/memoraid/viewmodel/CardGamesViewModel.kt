package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.CardGame
import com.example.memoraid.models.User
import com.example.memoraid.repository.CardGameRepository
import com.example.memoraid.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardGamesViewModel @Inject constructor(
    private val cardGameRepository: CardGameRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _weeklyActivity = MutableStateFlow<List<CardGame>>(emptyList())
    val weeklyActivity: StateFlow<List<CardGame>> = _weeklyActivity

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getUser()
        }
    }

    fun loadLastPlayedGames(userId: String) {
        viewModelScope.launch {
            _weeklyActivity.value = cardGameRepository.loadLastPlayedGames(userId)
        }
    }

    fun loadCardGameLevels(userId: String, onSuccess: (List<Map<String, Any>>) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            cardGameRepository.getCardGameLevels(userId, onSuccess, onFailure)
        }
    }

    fun getTotalGameScore(userId: String, onSuccess: (Long) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            cardGameRepository.getTotalGameScore(userId, onSuccess, onFailure)
        }
    }

    fun getBestScorePerLevel(userId: String, level: String, onSuccess: (Long) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            cardGameRepository.getBestScorePerLevel(userId, level, onSuccess, onFailure)
        }
    }

    fun getTotalGamesPerLevel(userId: String, level: String, onSuccess: (Long) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            cardGameRepository.getTotalGamesPerLevel(userId, level, onSuccess, onFailure)
        }
    }
}
