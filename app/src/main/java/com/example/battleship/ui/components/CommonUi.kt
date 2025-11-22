package com.example.battleship.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.battleship.R

@Composable
fun Background(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.pxfuel),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

@Composable
fun GameInvitation(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Game Invitation Alert")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Decline")
            }
        }
    )
}

@Composable
fun PlayerRow(player: String, onChallengeClick: () -> Unit) {
    // Split the player string into name and status
    val parts = player.split(" - ")
    val playerName = parts.getOrNull(0) ?: ""
    val status = parts.getOrNull(1) ?: ""

    val statusColor = if (status.equals("online", ignoreCase = true)) Color.Green else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            // Add a semi-transparent overlay for the row
            .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.small)
            .border(1.dp, Color.White, shape = MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = playerName, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = status, fontSize = 14.sp, color = statusColor)
        }
        Button(
            onClick = {
                onChallengeClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Challenge")
        }
    }
}

@Composable
fun GameGridView(
    grid: List<List<String>>,
    modifier: Modifier = Modifier,
    onCellClick: ((Int, Int) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = modifier
            .aspectRatio(1f) // Keep the grid square
            .border(2.dp, Color.White)
            .background(Color.Blue.copy(alpha = 0.3f))
    ) {
        items(100) { index ->
            val row = index / 10
            val col = index % 10
            val cellStatus = grid[row][col]

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.5f))
                    .background(
                        when (cellStatus) {
                            "S" -> Color.Gray // Ship
                            "H" -> Color.Red // Hit
                            "M" -> Color.White // Miss
                            else -> Color.Transparent // Water
                        }
                    )
                    .clickable(enabled = onCellClick != null) {
                        onCellClick?.invoke(row, col)
                    }
            )
        }
    }
}
