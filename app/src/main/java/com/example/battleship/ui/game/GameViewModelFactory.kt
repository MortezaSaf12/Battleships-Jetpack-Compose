package com.example.battleship.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.battleship.data.BattleshipRepository

class GameViewModelFactory(
    private val repository: BattleshipRepository,
    private val gameId: String,
    private val playerName: String,
    private val opponentName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository, gameId, playerName, opponentName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
