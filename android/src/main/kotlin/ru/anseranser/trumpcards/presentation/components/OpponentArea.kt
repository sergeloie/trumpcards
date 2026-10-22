package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anseranser.engine.Card
import ru.anseranser.trumpcards.R
import ru.anseranser.trumpcards.domain.OpponentState

@Composable
fun OpponentArea(
    opponent: OpponentState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = suitSymbol(opponent.suit),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = suitColor(opponent.suit),
            modifier = Modifier.padding(bottom = 2.dp),
        )
        if (opponent.isEliminated) {
            Text(
                text = stringResource(R.string.eliminated),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(4.dp),
            )
        } else {
            CardBack(
                cardCount = opponent.cardCount,
                modifier = Modifier
                    .width(56.dp)
                    .height(78.dp),
            )
        }
    }
}

private fun suitSymbol(suit: Card.Suit): String = when (suit) {
    Card.Suit.SPADES -> "♠"
    Card.Suit.CLUBS -> "♣"
    Card.Suit.DIAMONDS -> "♦"
    Card.Suit.HEARTS -> "♥"
}
private fun suitColor(suit: Card.Suit): Color = when (suit) {
    Card.Suit.SPADES, Card.Suit.CLUBS -> Color.Black
    Card.Suit.DIAMONDS, Card.Suit.HEARTS -> Color.Red
}
