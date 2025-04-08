package com.example.memoraid.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.Album
import com.example.memoraid.repository.AlbumRepository
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
}