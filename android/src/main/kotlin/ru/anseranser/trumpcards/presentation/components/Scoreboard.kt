package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.anseranser.model.Card

@Composable
fun Scoreboard(
    scoreboard: Map<Card.Suit, List<Card>>,
    modifier: Modifier = Modifier,
) {
    val suits = listOf(Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS)

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suits.forEach { suit ->
            val stack = scoreboard[suit] ?: emptyList()
            val topCard = stack.firstOrNull()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (topCard != null) {
                    CardView(
                        card = topCard,
                        modifier = Modifier
                            .width(40.dp)
                            .height(56.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF37474F)),
                    )
                }
            }
        }
    }
}
