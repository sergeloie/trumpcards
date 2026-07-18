package ru.anseranser.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.BackgroundDark
import ru.anseranser.android.ui.theme.GoldAccent
import ru.anseranser.android.ui.theme.TableGreen
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

@Composable
fun GameOverScreen(
    winnerSuit: Card.Suit?,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            style = MaterialTheme.typography.headlineLarge,
            color = GoldAccent
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (winnerSuit != null) {
            Text(
                text = "Winner: ${winnerSuit.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = TableGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "PLAY AGAIN",
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Button(
                onClick = onMainMenu,
                colors = ButtonDefaults.buttonColors(containerColor = TableGreen.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "MAIN MENU",
                    color = TextWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
