package com.example.battleship

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.battleship.data.BattleshipRepository
import com.example.battleship.ui.game.GameBoardScreen
import com.example.battleship.ui.game.GameViewModel
import com.example.battleship.ui.game.GameViewModelFactory
import com.example.battleship.ui.lobby.LobbyScreen
import com.example.battleship.ui.lobby.LobbyViewModel
import com.example.battleship.ui.lobby.LobbyViewModelFactory
import com.example.battleship.ui.main.MainScreen
import com.example.battleship.ui.main.MainViewModel
import com.example.battleship.ui.main.MainViewModelFactory

@Composable
fun Navigation() {
    val navController = rememberNavController()
    // Manual dependency injection since Hilt is not used
    val repository = remember { BattleshipRepository() }

    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository)
            )
            MainScreen(viewModel, navController)
        }
        composable("LobbyScreen/{username}") { backStackEntry ->
            val loggedInUsername = backStackEntry.arguments?.getString("username").orEmpty()
            val context = LocalContext.current
            
            if (loggedInUsername.isBlank()) {
                Toast.makeText(context, "Invalid username", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } else {
                val viewModel: LobbyViewModel = viewModel(
                    factory = LobbyViewModelFactory(repository, loggedInUsername)
                )
                LobbyScreen(viewModel, navController, loggedInUsername)
            }
        }
        composable("GameBoardScreen?gameId={gameId}&playerName={playerName}&opponentName={opponentName}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
            val playerName = backStackEntry.arguments?.getString("playerName").orEmpty()
            val opponentName = backStackEntry.arguments?.getString("opponentName").orEmpty()
            
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModelFactory(repository, gameId, playerName, opponentName)
            )
            
            GameBoardScreen(viewModel, navController, playerName, opponentName)
        }
    }
}
