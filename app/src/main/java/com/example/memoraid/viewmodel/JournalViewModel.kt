package com.example.memoraid.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.Journal
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

    private val _journals = MutableStateFlow<List<Journal>>(emptyList())
    val journals: StateFlow<List<Journal>> get() = _journals

    private val _journalDetails = MutableStateFlow<Journal?>(null)
    val journalDetails: StateFlow<Journal?> get() = _journalDetails

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> get() = _isSaving

    fun loadJournalDetails(journalId: String) {
        viewModelScope.launch {
            val journal = repository.loadJournalDetails(journalId)
            _journalDetails.value = journal
        }
    }

    suspend fun saveJournalDetails(journal: Journal): Boolean {
        _isSaving.value = true
        return try {
            val success = repository.saveJournalDetails(journal)
            _isSaving.value = false
            success
        } catch (e: Exception) {
            _isSaving.value = false
            false
        }
    }

    fun removeImageFromFirestore(imageUri: String) {
        viewModelScope.launch {
            repository.removeImageFromFirestore(imageUri)
        }
    }

    fun removeImageFromStorage(imageUri: String) {
        viewModelScope.launch {
            repository.removeImageFromStorage(imageUri)
        }
    }

    fun uploadImageToStorage(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            val uploadedUri = repository.uploadImageToStorage(uri)
            if (uploadedUri != null) {
                onSuccess(uploadedUri)
            } else {
                onFailure(Exception("Failed to upload image"))
            }
        }
    }

    fun checkIfImageExistsInStorage(imageUri: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = repository.checkIfImageExistsInStorage(imageUri)
            onResult(exists)
        }
    }
}