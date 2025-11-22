package com.example.battleship.ui.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val playerGrid: List<List<String>> = List(10) { List(10) { "W" } },
    val opponentGrid: List<List<String>> = List(10) { List(10) { "W" } },
    val isShipPlacementPhase: Boolean = true,
    val currentShipIndex: Int = 0,
    val isPlayerReady: Boolean = false,
    val startPoint: Pair<Int, Int>? = null,
    val endPoint: Pair<Int, Int>? = null,
    val isPlayerOneTurn: Boolean = true,
    val gameWon: Boolean = false,
    val ships: List<Pair<String, Int>> = listOf(
        "Carrier" to 4,
        "Battleship" to 3,
        "Cruiser1" to 2,
        "Cruiser2" to 2,
        "Submarine" to 1,
        "Destroyer" to 1
    )
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

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
        if (currentState.isPlayerOneTurn) {
            val currentOpponentGrid = currentState.opponentGrid
            val cellContent = currentOpponentGrid[row][col]
            
            var newGrid = currentOpponentGrid
            var gameWon = currentState.gameWon

            when (cellContent) {
                "W" -> {
                    newGrid = updateGrid(currentOpponentGrid, row, col, "M")
                }
                "S" -> {
                    newGrid = updateGrid(currentOpponentGrid, row, col, "H")
                    if (checkGameState(newGrid)) {
                        gameWon = true
                    }
                }
            }
            
            _uiState.update {
                it.copy(
                    opponentGrid = newGrid,
                    gameWon = gameWon,
                    isPlayerOneTurn = false
                )
            }
        } else {
            // Simulate opponent turn switch back
             _uiState.update { it.copy(isPlayerOneTurn = true) }
        }
    }

    private fun checkGameState(grid: List<List<String>>): Boolean {
        return grid.flatten().none { it == "S" }
    }

    fun onPlayerReady() {
        _uiState.update { it.copy(isPlayerReady = true) }
    }
}
