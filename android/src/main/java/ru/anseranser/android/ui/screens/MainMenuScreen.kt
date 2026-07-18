package ru.anseranser.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
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
            text = "TRUMPCARDS",
            style = MaterialTheme.typography.headlineLarge,
            color = GoldAccent
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u2660 \u2663 \u2666 \u2665",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartGame,
            colors = ButtonDefaults.buttonColors(
                containerColor = TableGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "START GAME",
                style = MaterialTheme.typography.titleMedium,
                color = GoldAccent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "4-player elimination trumps\n36-card deck (6\u2013A)",
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite.copy(alpha = 0.5f)
        )
    }
}
