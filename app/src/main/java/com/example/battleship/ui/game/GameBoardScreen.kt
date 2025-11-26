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
            .fillMaxSize()
            .padding(8.dp)
            .padding(top = 24.dp),
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

        // Game Phase
        if (uiState.isPlayerReady) {
            val statusText = when {
                uiState.gameWon -> "VICTORY"
                uiState.gameLost -> "DEFEAT"
                !uiState.isOpponentReady -> "Waiting for opponent to place ships..."
                uiState.isMyTurn -> "YOUR TURN"
                else -> "OPPONENT'S TURN"
            }
            
            Text(
                text = statusText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isMyTurn || uiState.gameWon) Color.Green else Color.Red,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Player's Grid (with ships) - at the top
            Text(
                text = "Your Ships",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GameGridView(
                grid = uiState.playerGrid,
                modifier = Modifier.size(200.dp) // Size of top grid
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                onCellClick = { row, col -> viewModel.onCellClick(row, col) },
                modifier = Modifier.size(300.dp) // Larger size for interaction
            )
            
            if (uiState.gameWon || uiState.gameLost) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Lobby")
                }
            }
        }
    }
}
