package ru.anseranser.trumpcards.presentation.screens.gameover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anseranser.model.Card

@Composable
fun GameOverScreen(
    winner: Card.Suit?,
    onPlayAgain: () -> Unit,
    onGoToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Game Over",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (winner != null) {
            val winnerName = when (winner) {
                Card.Suit.SPADES -> "You WIN!"
                else -> "${suitName(winner)} wins!"
            }
            Text(
                text = winnerName,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "Play Again",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGoToMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Text(
                text = "Main Menu",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun suitName(suit: Card.Suit): String = when (suit) {
    Card.Suit.SPADES -> "Spades"
    Card.Suit.CLUBS -> "Clubs"
    Card.Suit.DIAMONDS -> "Diamonds"
    Card.Suit.HEARTS -> "Hearts"
}
