package com.example.battleship.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.battleship.data.BattleshipRepository

class LobbyViewModelFactory(
    private val repository: BattleshipRepository,
    private val loggedInUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LobbyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LobbyViewModel(repository, loggedInUsername) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
