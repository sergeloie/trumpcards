package ru.anseranser.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.ScoreboardBg
import ru.anseranser.android.ui.theme.TextGold
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

@Composable
fun ScoreboardView(
    scoreboard: Map<Card.Suit, List<Card>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ScoreboardBg)
            .padding(8.dp)
    ) {
        Text(
            text = "SCOREBOARD",
            style = MaterialTheme.typography.labelSmall,
            color = TextGold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        for (suit in Card.Suit.entries) {
            val stack = scoreboard[suit]
            val topCard = stack?.firstOrNull()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
            ) {
                Text(
                    text = suitSymbol(suit),
                    style = MaterialTheme.typography.bodySmall,
                    color = suitColor(suit),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = topCard?.rank?.name ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "${stack?.size ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun suitSymbol(suit: Card.Suit) = when (suit) {
    Card.Suit.SPADES -> "\u2660"
    Card.Suit.CLUBS -> "\u2663"
    Card.Suit.DIAMONDS -> "\u2666"
    Card.Suit.HEARTS -> "\u2665"
}

private fun suitColor(suit: Card.Suit) = when (suit) {
    Card.Suit.SPADES -> TextWhite
    Card.Suit.CLUBS -> TextWhite
    Card.Suit.DIAMONDS -> TextGold
    Card.Suit.HEARTS -> TextGold
}
