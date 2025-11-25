package com.example.battleship.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship.data.BattleshipRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LobbyUiState(
    val players: List<String> = emptyList(),
    val incomingChallenge: Triple<String, String, String>? = null,
    val outgoingChallengeId: String? = null,
    val outgoingChallengeOpponent: String? = null,
    val navigateToGame: Triple<String, String, String>? = null, // playerName, opponentName, gameId
    val error: String? = null
)

class LobbyViewModel(
    private val repository: BattleshipRepository,
    private val loggedInUsername: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private var challengeListener: ListenerRegistration? = null
    private var outgoingChallengeListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            repository.getPlayersFlow(loggedInUsername).collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
        startListeningForChallenges()
    }

    fun setPlayerStatus(status: String) {
        repository.setPlayerStatus(loggedInUsername, status)
    }

    private fun startListeningForChallenges() {
        challengeListener = repository.listenForChallenges(loggedInUsername) { id, from, to ->
            _uiState.update { it.copy(incomingChallenge = Triple(id, from, to)) }
        }
    }

    fun onChallengePlayer(opponentName: String) {
        repository.sendChallenge(
            fromPlayer = loggedInUsername,
            toPlayer = opponentName,
            onSuccess = { challengeId ->
                _uiState.update {
                    it.copy(
                        outgoingChallengeId = challengeId,
                        outgoingChallengeOpponent = opponentName
                    )
                }
                listenToOutgoingChallenge(challengeId, opponentName)
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = "Failed to send challenge: ${e.message}") }
            }
        )
    }

    private fun listenToOutgoingChallenge(challengeId: String?, opponentName: String) {
        if (challengeId == null) return
        
        viewModelScope.launch {
             repository.listenToChallengeUpdates(challengeId).collect { data ->
                val status = data["status"] as? String
                val fromPlayer = data["fromPlayer"] as? String
                val toPlayer = data["toPlayer"] as? String

                if (status == "accepted" && fromPlayer == loggedInUsername && toPlayer == opponentName) {
                    val gameId = data["gameId"] as? String
                    if (gameId != null) {
                        _uiState.update { it.copy(navigateToGame = Triple(fromPlayer, toPlayer, gameId)) }
                    }
                }
             }
        }
    }

    fun onAcceptChallenge(challengeId: String, fromPlayer: String) {
        repository.acceptChallengeWithGame(
            challengeId = challengeId,
            playerA = fromPlayer, // Challenger (Player 1)
            playerB = loggedInUsername, // Acceptor (Player 2)
            onSuccess = { gameId ->
                _uiState.update {
                    it.copy(
                        incomingChallenge = null,
                        navigateToGame = Triple(loggedInUsername, fromPlayer, gameId)
                    )
                }
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = "Error accepting challenge: ${e.message}") }
            }
        )
    }

    fun onDeclineChallenge(challengeId: String) {
        repository.declineChallenge(challengeId)
        _uiState.update { it.copy(incomingChallenge = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToGame = null) }
    }
    
    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        challengeListener?.remove()
        outgoingChallengeListener?.remove()
    }
}
