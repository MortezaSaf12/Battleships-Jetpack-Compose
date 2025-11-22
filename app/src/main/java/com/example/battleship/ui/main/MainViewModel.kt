package com.example.battleship.ui.main

import androidx.lifecycle.ViewModel
import com.example.battleship.data.BattleshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToLobby: Boolean = false
)

class MainViewModel(private val repository: BattleshipRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun onJoinLobby() {
        val username = _uiState.value.username.trim()
        if (username.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a valid Username") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        repository.checkAndAddPlayer(
            username = username,
            onPlayerExists = {
                _uiState.update { it.copy(isLoading = false, navigateToLobby = true) }
            },
            onPlayerAdded = {
                _uiState.update { it.copy(isLoading = false, navigateToLobby = true) }
            },
            onError = { errorMessage ->
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        )
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToLobby = false) }
    }
    
    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
