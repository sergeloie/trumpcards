package ru.anseranser.android.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.anseranser.android.engine.ComposeGameEngine
import ru.anseranser.android.ui.components.GameLogView
import ru.anseranser.android.ui.components.OpponentRow
import ru.anseranser.android.ui.components.PlayerHandView
import ru.anseranser.android.ui.components.PotView
import ru.anseranser.android.ui.components.ScoreboardView
import ru.anseranser.android.ui.components.TrumpBadge
import ru.anseranser.android.ui.theme.BackgroundDark
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

private const val TAG = "GameScreen"

@Composable
fun GameScreen(
    onGameOver: (Card.Suit?) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "GameScreen composed")
    val engine = remember { ComposeGameEngine() }
    val snapshot by engine.snapshot.collectAsState()

    DisposableEffect(Unit) {
        Log.d(TAG, "Starting game engine")
        try {
            engine.startGame()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start game engine", e)
        }
        onDispose {
            Log.d(TAG, "Stopping game engine")
            engine.stop()
        }
    }

    LaunchedEffect(snapshot.isGameOver) {
        if (snapshot.isGameOver) {
            Log.d(TAG, "Game over, navigating")
            onGameOver(snapshot.winnerSuit)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 16.dp)
    ) {
        // Top bar: Trump badge
        TrumpBadge(
            trump = snapshot.trump,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )

        // Status text
        Text(
            text = snapshot.statusText,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = TextWhite.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Main area: scoreboard | pot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            ScoreboardView(
                scoreboard = snapshot.scoreboard,
                modifier = Modifier.width(100.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            PotView(
                pot = snapshot.pot,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Spacer(modifier = Modifier.width(100.dp))
        }

        OpponentRow(
            opponents = snapshot.opponents,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        PlayerHandView(
            hand = snapshot.humanHand,
            validChoices = snapshot.humanValidChoices,
            isHumanTurn = snapshot.isHumanTurn,
            onCardClick = { engine.selectCard(it) },
            modifier = Modifier.fillMaxWidth()
        )

        GameLogView(
            logLines = snapshot.logLines,
            modifier = Modifier
        )
    }
}
