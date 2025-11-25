package com.example.battleship.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import com.google.firebase.firestore.FieldValue

class BattleshipRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun checkAndAddPlayer(
        username: String,
        onPlayerExists: () -> Unit,
        onPlayerAdded: () -> Unit,
        onError: (String) -> Unit
    ) {
        val playersCollection = firestore.collection("players")

        playersCollection
            .whereEqualTo("name", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    playersCollection
                        .document(snapshot.documents[0].id)
                        .update("status", "online")
                        .addOnSuccessListener { onPlayerExists() }
                        .addOnFailureListener { onError("Error updating player status") }
                } else {
                    playersCollection
                        .add(
                            mapOf(
                                "name" to username,
                                "status" to "online",
                                "playerId" to UUID.randomUUID().toString()
                            )
                        )
                        .addOnSuccessListener { onPlayerAdded() }
                        .addOnFailureListener { exception ->
                            onError("Error adding player: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                onError("Error checking player: ${exception.message}")
            }
    }

    fun setPlayerStatus(username: String, status: String) {
        firestore.collection("players")
            .whereEqualTo("name", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    snapshot.documents[0].reference.update("status", status)
                        .addOnFailureListener { error ->
                            Log.e("Error", "Error updating player status: ${error.message}")
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e("Error", "Error fetching player document: ${error.message}")
            }
    }

    fun getPlayersFlow(loggedInUsername: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val players = snapshot?.documents?.mapNotNull {
                    val name = it.getString("name")
                    val status = it.getString("status")
                    if (name != null && status != null && name != loggedInUsername) "$name - $status" else null
                } ?: emptyList()
                trySend(players)
            }
        awaitClose { listener.remove() }
    }

    fun sendChallenge(
        fromPlayer: String,
        toPlayer: String,
        onSuccess: (challengeId: String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val challenge = mapOf(
            "fromPlayer" to fromPlayer,
            "toPlayer" to toPlayer,
            "status" to "pending"
        )

        firestore.collection("challenges")
            .add(challenge)
            .addOnSuccessListener { docRef ->
                onSuccess(docRef.id)
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun listenForChallenges(
        loggedInUsername: String,
        onIncomingChallenge: (String, String, String) -> Unit
    ): ListenerRegistration {
        return firestore.collection("challenges")
            .whereEqualTo("toPlayer", loggedInUsername)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChallengeError", "Error listening to challenges: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val fromPlayer = doc.getString("fromPlayer")
                    val toPlayer = doc.getString("toPlayer")
                    val status = doc.getString("status")

                    if (fromPlayer != null && toPlayer != null && status == "pending") {
                        Triple(id, fromPlayer, toPlayer)
                    } else null
                }?.firstOrNull()?.let { (id, fromPlayer, toPlayer) ->
                    onIncomingChallenge(id, fromPlayer, toPlayer)
                }
            }
    }
    
    fun listenToChallengeUpdates(challengeId: String): Flow<Map<String, Any?>> = callbackFlow {
         val listener = firestore.collection("challenges").document(challengeId)
            .addSnapshotListener { docSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val data = docSnapshot?.data
                if (data != null) {
                    trySend(data)
                }
            }
        awaitClose { listener.remove() }
    }

    fun acceptChallenge(
        challengeId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("challenges").document(challengeId)
            .update("status", "accepted")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun declineChallenge(challengeId: String) {
        firestore.collection("challenges").document(challengeId)
            .update("status", "declined")
            .addOnFailureListener { exception ->
                Log.e("ChallengeError", "Error declining challenge: ${exception.message}")
            }
    }
    }

    // Game Logic
    fun createGame(
        playerA: String,
        playerB: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val newGame = mapOf(
            "player1" to playerA,
            "player2" to playerB,
            "player1Ready" to false,
            "player2Ready" to false,
            "turn" to playerA, // Player A starts
            "status" to "active",
            "winner" to "",
            "moves" to emptyList<Map<String, Any>>()
        )

        firestore.collection("games")
            .add(newGame)
            .addOnSuccessListener { docRef -> onSuccess(docRef.id) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun listenForActiveGame(
        playerName: String,
        onGameFound: (String) -> Unit
    ): ListenerRegistration {
    
        return firestore.collection("games")
            .whereArrayContains("players_index", playerName)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documents?.firstOrNull()?.let { doc ->
                    onGameFound(doc.id)
                }
            }
    }
    
    fun acceptChallengeWithGame(
        challengeId: String,
        playerA: String,
        playerB: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // 1. Create Game
        val newGame = mapOf(
            "player1" to playerA,
            "player2" to playerB,
            "player1Ready" to false,
            "player2Ready" to false,
            "turn" to playerA,
            "status" to "setup", 
            "winner" to ""
        )
        
        firestore.collection("games")
            .add(newGame)
            .addOnSuccessListener { gameDoc ->
                // 2. Update Challenge with Game ID and status
                firestore.collection("challenges").document(challengeId)
                    .update(mapOf("status" to "accepted", "gameId" to gameDoc.id))
                    .addOnSuccessListener { onSuccess(gameDoc.id) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenForGame(gameId: String): Flow<Map<String, Any?>> = callbackFlow {
        val listener = firestore.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data ?: emptyMap())
                }
            }
        awaitClose { listener.remove() }
    }

    fun updatePlayerBoard(
        gameId: String,
        playerField: String, 
        board: List<List<String>>,
        readyField: String,
        onSuccess: () -> Unit
    ) {
        val serializedBoard = board.map { row -> row.joinToString("") }
        
        firestore.collection("games").document(gameId)
            .update(
                mapOf(
                    playerField to serializedBoard,
                    readyField to true
                )
            )
            .addOnSuccessListener { onSuccess() }
    }

    fun makeMove(
        gameId: String,
        move: Map<String, Any>
    ) {
        firestore.collection("games").document(gameId)
            .update("moves", FieldValue.arrayUnion(move))
    }
    
    fun updateGameState(gameId: String, updates: Map<String, Any>) {
        firestore.collection("games").document(gameId).update(updates)
    }
}
