package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Text(
                    text = suitSymbol(suit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = suitColor(suit),
                )
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

private fun suitSymbol(suit: Card.Suit): String = when (suit) {
    Card.Suit.SPADES -> "\u2660"
    Card.Suit.CLUBS -> "\u2663"
    Card.Suit.DIAMONDS -> "\u2666"
    Card.Suit.HEARTS -> "\u2665"
}

private fun suitColor(suit: Card.Suit): Color = when (suit) {
    Card.Suit.SPADES, Card.Suit.CLUBS -> Color.Black
    Card.Suit.DIAMONDS, Card.Suit.HEARTS -> Color.Red
}
