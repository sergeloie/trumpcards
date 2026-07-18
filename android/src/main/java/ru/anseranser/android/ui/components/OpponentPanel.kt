package ru.anseranser.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.engine.ComposeSnapshot
import ru.anseranser.android.ui.theme.OpponentBg
import ru.anseranser.android.ui.theme.TextGold
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

@Composable
fun OpponentPanel(
    opponent: ComposeSnapshot.OpponentView,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OpponentBg)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CardBack(suit = opponent.trump, modifier = Modifier.size(48.dp, 66.dp))

        Text(
            text = opponent.trump.name,
            style = MaterialTheme.typography.labelSmall,
            color = suitColor(opponent.trump),
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = "${opponent.cardCount} cards",
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite
        )
    }
}

@Composable
fun OpponentRow(
    opponents: List<ComposeSnapshot.OpponentView>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        opponents.forEach { opponent ->
            OpponentPanel(opponent = opponent)
        }
    }
}

private fun suitColor(suit: Card.Suit) = when (suit) {
    Card.Suit.SPADES -> TextWhite
    Card.Suit.CLUBS -> TextWhite
    Card.Suit.DIAMONDS -> TextGold
    Card.Suit.HEARTS -> TextGold
}
