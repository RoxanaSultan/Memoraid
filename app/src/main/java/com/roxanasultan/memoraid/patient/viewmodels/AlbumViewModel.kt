package com.roxanasultan.memoraid.patient.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Album
import com.roxanasultan.memoraid.repositories.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repository: AlbumRepository
) : ViewModel() {

    private val _albums = MutableStateFlow<MutableList<Album>>(mutableListOf())
    val albums: StateFlow<MutableList<Album>> get() = _albums

    private val _albumDetails = MutableStateFlow<Album?>(null)
    val albumDetails: StateFlow<Album?> get() = _albumDetails

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> get() = _isSaving

    fun loadAlbums() {
        viewModelScope.launch {
            _albums.value = repository.loadAlbums().toMutableList()
        }
    }

    fun createAlbum(type: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val id = repository.createAlbum(type)
            if (id != null) onSuccess(id) else onFailure()
        }
    }

    fun deleteAlbum(albumId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteAlbum(albumId)
            if (result) {
                loadAlbums()
            }
            onComplete()
        }
    }

    fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            _albumDetails.value = repository.loadAlbumDetails(albumId)
        }
    }

    suspend fun saveAlbumDetails(album: Album): Boolean {
        _isSaving.value = true
        return try {
            val success = repository.saveAlbumDetails(album)
            _isSaving.value = false
            success
        } catch (e: Exception) {
            _isSaving.value = false
            false
        }
    }

    fun removeImageFromFirestore(image: String) {
        viewModelScope.launch {
            repository.removeImageFromFirestore(image)
        }
    }

    fun removeImageFromStorage(image: String) {
        viewModelScope.launch {
            repository.removeImageFromStorage(image)
        }
    }

    fun uploadImageToStorage(image: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            val uploadedImage = repository.uploadImageToStorage(image)
            if (uploadedImage != null) {
                onSuccess(uploadedImage)
            } else {
                onFailure(Exception("Failed to upload image"))
            }
        }
    }

    fun checkIfImageExistsInStorage(image: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = repository.checkIfImageExistsInStorage(image)
            onResult(exists)
        }
    }
}