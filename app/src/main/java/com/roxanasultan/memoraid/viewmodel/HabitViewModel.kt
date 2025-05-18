package com.roxanasultan.memoraid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxanasultan.memoraid.models.Habit
import com.roxanasultan.memoraid.models.User
import com.roxanasultan.memoraid.repository.HabitRepository
import com.roxanasultan.memoraid.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitsRepository: HabitRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _habits = MutableStateFlow<MutableList<Habit>>(mutableListOf())
    val habits: StateFlow<MutableList<Habit>> get() = _habits

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _selectedPatient = MutableStateFlow<User?>(null)
    val selectedPatient: StateFlow<User?> get() = _selectedPatient

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getUser()
        }
    }

    fun loadSelectedPatient() {
        viewModelScope.launch {
            _selectedPatient.value = userRepository.getCurrentPatient()
        }
    }

    fun loadHabits(userId: String) {
        viewModelScope.launch {
            _habits.value = habitsRepository.loadHabits(userId).toMutableList()
        }
    }

    fun addHabit(habit: Habit, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val id = habitsRepository.addHabit(habit)
            if (id != null) onSuccess(id) else onFailure()
        }
    }

    fun updateHabit(habit: Habit, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = habitsRepository.updateHabit(habit)
            if (success) onSuccess() else onFailure()
        }
    }

    fun deleteHabit(habitId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = habitsRepository.deleteHabit(habitId)
            onComplete(success)
        }
    }

    fun updateHabitCheckedDates(habitId: String, updatedDates: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = habitsRepository.updateHabitCheckedDates(habitId, updatedDates)
            onComplete(success)
        }
    }
}