package com.example.battleship.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship.data.BattleshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val playerGrid: List<List<String>> = List(10) { List(10) { "W" } },
    val opponentGrid: List<List<String>> = List(10) { List(10) { "W" } }, // View of the opponent's board (shots fired)
    val trueOpponentBoard: List<List<String>>? = null, // Actual opponent board (for hit testing)
    val isShipPlacementPhase: Boolean = true,
    val currentShipIndex: Int = 0,
    val isPlayerReady: Boolean = false,
    val isOpponentReady: Boolean = false,
    val startPoint: Pair<Int, Int>? = null,
    val endPoint: Pair<Int, Int>? = null,
    val isMyTurn: Boolean = false,
    val gameWon: Boolean = false,
    val gameLost: Boolean = false,
    val ships: List<Pair<String, Int>> = listOf(
        "Carrier" to 4,
        "Battleship" to 3,
        "Cruiser1" to 2,
        "Cruiser2" to 2,
        "Submarine" to 1,
        "Destroyer" to 1
    )
)

class GameViewModel(
    private val repository: BattleshipRepository,
    private val gameId: String,
    private val playerName: String,
    private val opponentName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        startListeningToGame()
    }

    private fun startListeningToGame() {
        viewModelScope.launch {
            repository.listenForGame(gameId).collect { gameData ->
                val status = gameData["status"] as? String
                val turn = gameData["turn"] as? String
                val player1 = gameData["player1"] as? String
                val player2 = gameData["player2"] as? String
                
                // Determine if I am player 1 or 2
                val isPlayer1 = playerName == player1
                val myBoardField = if (isPlayer1) "player1Board" else "player2Board"
                val opponentBoardField = if (isPlayer1) "player2Board" else "player1Board"
                val myReadyField = if (isPlayer1) "player1Ready" else "player2Ready"
                val opponentReadyField = if (isPlayer1) "player2Ready" else "player1Ready"

                val opponentReady = gameData[opponentReadyField] as? Boolean ?: false
                val myReady = gameData[myReadyField] as? Boolean ?: false
                val moves = gameData["moves"] as? List<Map<String, Any>> ?: emptyList()
                val winner = gameData["winner"] as? String

                // Parse Opponent Board if available (to check hits)
                val rawOpponentBoard = gameData[opponentBoardField] as? List<String>
                val trueOpponentBoard = rawOpponentBoard?.map { rowStr ->
                    rowStr.map { it.toString() }
                }
                
                var currentMyGrid = _uiState.value.playerGrid
                var currentOpponentView = _uiState.value.opponentGrid

                // Apply moves
                moves.forEach { move ->
                    val player = move["player"] as? String
                    val row = (move["row"] as? Long)?.toInt() ?: 0
                    val col = (move["col"] as? Long)?.toInt() ?: 0
                    val result = move["result"] as? String ?: "M"

                    if (player == playerName) {
                        // I made this move -> Update opponent grid view
                        currentOpponentView = updateGrid(currentOpponentView, row, col, result)
                    } else {
                        // Opponent made this move -> Update my grid
                        currentMyGrid = updateGrid(currentMyGrid, row, col, result)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        isOpponentReady = opponentReady,
                        isMyTurn = (turn == playerName) && myReady && opponentReady,
                        trueOpponentBoard = trueOpponentBoard,
                        playerGrid = if (myReady) currentMyGrid else state.playerGrid,
                        opponentGrid = currentOpponentView,
                        gameWon = winner == playerName,
                        gameLost = winner != "" && winner != playerName
                    )
                }
            }
        }
    }

    // Helper to update grid at specific position
    private fun updateGrid(grid: List<List<String>>, row: Int, col: Int, value: String): List<List<String>> {
        return grid.mapIndexed { r, rowList ->
            if (r == row) {
                rowList.mapIndexed { c, cell -> if (c == col) value else cell }
            } else {
                rowList
            }
        }
    }

    fun onCellClick(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.isShipPlacementPhase) {
            handleShipPlacementClick(row, col)
        } else {
            handleGamePlayClick(row, col)
        }
    }

    private fun handleShipPlacementClick(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.startPoint == null) {
            _uiState.update { it.copy(startPoint = row to col) }
        } else {
            _uiState.update { it.copy(endPoint = row to col) }
            attemptPlaceShip()
        }
    }

    private fun attemptPlaceShip() {
        val currentState = _uiState.value
        val start = currentState.startPoint ?: return
        val end = currentState.endPoint ?: return
        val (currentShip, currentSize) = currentState.ships[currentState.currentShipIndex]

        if (placeShip(start, end, currentSize)) {
            if (currentState.currentShipIndex < currentState.ships.size - 1) {
                _uiState.update {
                    it.copy(
                        currentShipIndex = it.currentShipIndex + 1,
                        startPoint = null,
                        endPoint = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isShipPlacementPhase = false,
                        startPoint = null,
                        endPoint = null
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    startPoint = null,
                    endPoint = null
                )
            }
        }
    }

    private fun placeShip(startPoint: Pair<Int, Int>, endPoint: Pair<Int, Int>, shipSize: Int): Boolean {
        val (startRow, startCol) = startPoint
        val (endRow, endCol) = endPoint
        val grid = _uiState.value.playerGrid.map { it.toMutableList() }.toMutableList()

        if (shipSize == 1) {
            if (grid[startRow][startCol] == "W" && isWaterAroundCell(grid, startRow, startCol)) {
                grid[startRow][startCol] = "S"
                _uiState.update { it.copy(playerGrid = grid) }
                return true
            }
            return false
        }

        val isHorizontal = startRow == endRow
        val isVertical = startCol == endCol

        if (!isHorizontal && !isVertical) return false

        val cells = if (isHorizontal) {
            (minOf(startCol, endCol)..maxOf(startCol, endCol)).map { startRow to it }
        } else {
            (minOf(startRow, endRow)..maxOf(startRow, endRow)).map { it to startCol }
        }

        if (cells.size != shipSize || cells.any { (r, c) -> grid[r][c] != "W" }) {
            return false
        }

        if (!isWaterAroundShip(grid, cells)) {
            return false
        }

        cells.forEach { (r, c) -> grid[r][c] = "S" }
        _uiState.update { it.copy(playerGrid = grid) }
        return true
    }

    private fun isWaterAroundCell(grid: List<List<String>>, row: Int, col: Int): Boolean {
        val boardSize = 10
        return if (row in 0 until boardSize && col in 0 until boardSize) {
            grid[row][col] == "W"
        } else {
            true
        }
    }

    private fun isWaterAroundShip(grid: List<List<String>>, cells: List<Pair<Int, Int>>): Boolean {
        for ((r, c) in cells) {
            if (!isWaterAroundCell(grid, r - 1, c) ||
                !isWaterAroundCell(grid, r + 1, c) ||
                !isWaterAroundCell(grid, r, c - 1) ||
                !isWaterAroundCell(grid, r, c + 1)) {
                return false
            }
        }
        return true
    }

    private fun handleGamePlayClick(row: Int, col: Int) {
        val currentState = _uiState.value
        
        // Validation: Must be my turn, game not over, opponent ready, and cell not already shot
        if (!currentState.isMyTurn || currentState.gameWon || currentState.gameLost || !currentState.isOpponentReady) {
            return
        }
        
        if (currentState.opponentGrid[row][col] != "W") {
            return // Already shot here
        }

        // Determine Hit or Miss. Looking at the trueOpponentBoard, if it's null (not synced yet), we can't shoot.
        val trueBoard = currentState.trueOpponentBoard ?: return
        val targetCell = trueBoard[row][col]
        val result = if (targetCell == "S") "H" else "M"

        // Reflect move to firestore
        val move = mapOf(
            "player" to playerName,
            "row" to row,
            "col" to col,
            "result" to result
        )
        
        repository.makeMove(gameId, move)
    
        val updates = mutableMapOf<String, Any>("turn" to opponentName) // Update turn
        
        // Check for Win        
        var hitCount = currentState.opponentGrid.flatten().count { it == "H" }
        if (result == "H") hitCount++
        
        if (hitCount >= 13) {
            updates["winner"] = playerName
            updates["status"] = "finished"
        }
        repository.updateGameState(gameId, updates)
    }

    // Determine field names, update both fields conditionally
    fun onPlayerReady() {
        val currentState = _uiState.value
        if (currentState.isPlayerReady) return

        viewModelScope.launch {
             val gameData = repository.listenForGame(gameId).firstOrNull() ?: return@launch
             val p1 = gameData["player1"] as? String
             val isPlayer1 = p1 == playerName
             
             val boardField = if (isPlayer1) "player1Board" else "player2Board"
             val readyField = if (isPlayer1) "player1Ready" else "player2Ready"
             
             repository.updatePlayerBoard(
                 gameId, 
                 boardField, 
                 currentState.playerGrid,
                 readyField
             ) {
                 _uiState.update { it.copy(isPlayerReady = true) }
             }
        }
    }
}
