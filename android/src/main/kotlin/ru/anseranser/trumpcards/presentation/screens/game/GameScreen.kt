package ru.anseranser.trumpcards.presentation.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ru.anseranser.model.Card
import ru.anseranser.trumpcards.domain.GameIntent
import ru.anseranser.trumpcards.domain.GamePhase
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.presentation.components.OpponentArea
import ru.anseranser.trumpcards.presentation.components.PlayerHand
import ru.anseranser.trumpcards.presentation.components.PotArea
import ru.anseranser.trumpcards.presentation.components.Scoreboard
import ru.anseranser.trumpcards.util.CardImageMapper

@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onGameFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/${CardImageMapper.backgroundPath()}")
            .crossfade(true)
            .build(),
    )

    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.GAME_OVER) {
            onGameFinished()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top: Scoreboard
            Scoreboard(scoreboard = state.scoreboard)

            Spacer(modifier = Modifier.height(4.dp))

            // Top opponent (DIAMONDS)
            val topOpponent = state.opponents.find { it.suit == Card.Suit.DIAMONDS }
            if (topOpponent != null) {
                OpponentArea(opponent = topOpponent)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle area: Left bot, Pot, Right bot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left opponent (CLUBS)
                val leftOpponent = state.opponents.find { it.suit == Card.Suit.CLUBS }
                if (leftOpponent != null) {
                    OpponentArea(opponent = leftOpponent)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pot in the center
                PotArea(pot = state.pot)

                Spacer(modifier = Modifier.weight(1f))

                // Right opponent (HEARTS)
                val rightOpponent = state.opponents.find { it.suit == Card.Suit.HEARTS }
                if (rightOpponent != null) {
                    OpponentArea(opponent = rightOpponent)
                }
            }

            // Trump label
            state.trump?.let { trump ->
                Text(
                    text = "Your trump: ${trumpSymbol(trump)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }

            // Bottom: Player hand in 4 rows by suit
            PlayerHand(
                hand = state.humanHand,
                playableCards = state.inputRequest?.validCards ?: emptyList(),
                onCardClick = { card ->
                    onIntent(GameIntent.CardChosen(card))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun trumpSymbol(trump: Card.Suit): String = when (trump) {
    Card.Suit.SPADES -> "\u2660 SPADES"
    Card.Suit.CLUBS -> "\u2663 CLUBS"
    Card.Suit.DIAMONDS -> "\u2666 DIAMONDS"
    Card.Suit.HEARTS -> "\u2665 HEARTS"
}
