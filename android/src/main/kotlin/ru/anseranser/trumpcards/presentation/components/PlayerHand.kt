package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anseranser.model.Card

@Composable
fun PlayerHand(
    hand: List<Card>,
    playableCards: List<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suits = listOf(Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS)

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        suits.forEach { suit ->
            val suitCards = hand.filterNotNull()
                .filter { it.suit() == suit }
                .sortedBy { it.rank().value }

            if (suitCards.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-16).dp),
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
