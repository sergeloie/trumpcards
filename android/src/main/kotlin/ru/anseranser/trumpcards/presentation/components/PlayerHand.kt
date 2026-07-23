package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.anseranser.model.Card

@Composable
fun PlayerHand(
    hand: List<Card>,
    playableCards: List<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suits = listOf(Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS)

    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        val maxWidth = maxWidth

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            suits.forEach { suit ->
                val suitCards = hand.filterNotNull()
                    .filter { it.suit() == suit }
                    .sortedBy { it.rank().value }

                if (suitCards.isNotEmpty()) {
                    val needsOverlap = (suitCards.size * 52).dp > maxWidth
                    val spacing = if (needsOverlap) (-16).dp else 4.dp

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        suitCards.forEach { card ->
                            val isPlayable = playableCards.contains(card)
                            CardView(
                                card = card,
                                onClick = if (isPlayable) {{ onCardClick(card) }} else null,
                                isPlayable = isPlayable,
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(73.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
