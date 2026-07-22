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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            // Top zone: Scoreboard + top opponent
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Scoreboard(scoreboard = state.scoreboard)

                Spacer(modifier = Modifier.height(4.dp))

                val topOpponent = state.opponents.find { it.suit == Card.Suit.DIAMONDS }
                if (topOpponent != null) {
                    OpponentArea(opponent = topOpponent)
                }
            }

            // Center zone: always at screen center
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val leftOpponent = state.opponents.find { it.suit == Card.Suit.CLUBS }
                if (leftOpponent != null) {
                    OpponentArea(opponent = leftOpponent)
                }

                Spacer(modifier = Modifier.weight(1f))

                PotArea(pot = state.pot)

                Spacer(modifier = Modifier.weight(1f))

                val rightOpponent = state.opponents.find { it.suit == Card.Suit.HEARTS }
                if (rightOpponent != null) {
                    OpponentArea(opponent = rightOpponent)
                }
            }

            // Bottom zone: Player hand
            PlayerHand(
                hand = state.humanHand,
                playableCards = state.inputRequest?.validCards ?: emptyList(),
                onCardClick = { card ->
                    onIntent(GameIntent.CardChosen(card))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }
    }
}
