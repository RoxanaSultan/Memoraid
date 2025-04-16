package com.example.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoraid.models.Habit
import com.example.memoraid.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val _habits = MutableStateFlow<MutableList<Habit>>(mutableListOf())
    val habits: StateFlow<MutableList<Habit>> get() = _habits

    fun loadHabits() {
        viewModelScope.launch {
            _habits.value = repository.loadHabits().toMutableList()
        }
    }
}