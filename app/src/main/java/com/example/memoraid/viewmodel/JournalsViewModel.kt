package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.model.Journal
import com.example.memoraid.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

    private val _journals = MutableStateFlow<MutableList<Journal>>(mutableListOf())
    val journals: StateFlow<MutableList<Journal>> get() = _journals

    fun loadJournals() {
        viewModelScope.launch {
            _journals.value = repository.loadJournals().toMutableList()
        }
    }

    fun deleteJournal(journalId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteJournal(journalId)
            if (result) {
                loadJournals()
            }
            onComplete()
        }
    }

    fun createJournal(type: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val id = repository.createJournal(type)
            if (id != null) onSuccess(id) else onFailure()
        }
    }
}
