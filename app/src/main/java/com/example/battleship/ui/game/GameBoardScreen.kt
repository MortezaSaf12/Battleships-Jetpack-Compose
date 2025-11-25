package com.example.battleship.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.battleship.ui.components.GameGridView

@Composable
fun GameBoardScreen(
    viewModel: GameViewModel,
    navController: NavController,
    playerName: String,
    opponentName: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isShipPlacementPhase) {
            val (currentShip, currentSize) = uiState.ships[uiState.currentShipIndex]

            Text(
                text = "Place Your Ships",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Current Ship: $currentShip ($currentSize spaces)",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = if (uiState.startPoint == null) "Select the starting point" else "Select the ending point",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Player's Grid for ship placement
            GameGridView(
                grid = uiState.playerGrid,
                onCellClick = { row, col -> viewModel.onCellClick(row, col) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Ready Button
        if (!uiState.isShipPlacementPhase && !uiState.isPlayerReady) {
            Button(
                onClick = { viewModel.onPlayerReady() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Ready")
            }
        }

        // Gameplay Phase
        if (uiState.isPlayerReady) {
            val statusText = when {
                uiState.gameWon -> "VICTORY! You sank all ships!"
                uiState.gameLost -> "DEFEAT! Your ships were sunk."
                !uiState.isOpponentReady -> "Waiting for opponent to place ships..."
                uiState.isMyTurn -> "YOUR TURN! Fire at will!"
                else -> "OPPONENT'S TURN. Brace for impact!"
            }
            
            Text(
                text = statusText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isMyTurn || uiState.gameWon) Color.Green else Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Player's Grid (with ships) - at the top
            Text(
                text = "Your Ships",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GameGridView(
                grid = uiState.playerGrid,
                modifier = Modifier.size(300.dp) // Size of the top grid
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Opponent's Grid (empty for attacks) - at the bottom
            Text(
                text = "Opponent's Board",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Render Opponent Grid with masking logic (ships hidden)
            val maskedOpponentGrid = uiState.opponentGrid.map { row ->
                row.map { cell -> if (cell == "S") "W" else cell }
            }
            
            GameGridView(
                grid = maskedOpponentGrid,
                onCellClick = { row, col -> viewModel.onCellClick(row, col) }
            )
        }
    }
}
