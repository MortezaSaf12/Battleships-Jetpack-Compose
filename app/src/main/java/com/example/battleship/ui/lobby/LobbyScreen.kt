package com.example.battleship.ui.lobby

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.battleship.ui.components.Background
import com.example.battleship.ui.components.GameInvitation
import com.example.battleship.ui.components.PlayerRow

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    navController: NavController,
    loggedInUsername: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle observer for setting player online/offline
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setPlayerStatus("online")
                Lifecycle.Event.ON_STOP -> viewModel.setPlayerStatus("offline")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.navigateToGame) {
        uiState.navigateToGame?.let { (player, opponent) ->
            navController.navigate("GameBoardScreen?playerName=$player&opponentName=$opponent")
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onErrorShown()
        }
    }

    Background {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lobby",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Welcome, $loggedInUsername!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Online Players",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(uiState.players) { player ->
                        PlayerRow(player = player) {
                            val selectedOpponent = player.substringBefore(" - ")
                            viewModel.onChallengePlayer(selectedOpponent)
                        }
                    }
                }
            }
        }

        uiState.incomingChallenge?.let { (challengeId, challenger, _) ->
            GameInvitation(
                onDismissRequest = {
                    viewModel.onDeclineChallenge(challengeId)
                },
                onConfirmation = {
                    viewModel.onAcceptChallenge(challengeId, challenger)
                },
                dialogTitle = "Game Invitation",
                dialogText = "$challenger has challenged you to a game. Do you accept?",
                icon = Icons.Default.Notifications
            )
        }
    }
}
