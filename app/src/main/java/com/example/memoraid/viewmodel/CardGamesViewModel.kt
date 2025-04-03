package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.repository.CardGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardGamesViewModel @Inject constructor(
    private val cardGameRepository: CardGameRepository
) : ViewModel() {

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
}
