package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.anseranser.model.Card

@Composable
fun PotArea(
    pot: List<Card>,
    modifier: Modifier = Modifier,
) {
    val cards = pot.filterNotNull()
    if (cards.isEmpty()) return

    // Show at most the last 3 cards, with the last card fully visible on top
    val visible = if (cards.size > 3) cards.takeLast(3) else cards
    val halfWidth = (visible.lastIndex * 6)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        visible.forEachIndexed { index, card ->
            val xOff = (index * 12 - halfWidth).dp
            CardView(
                card = card,
                modifier = Modifier
                    .width(52.dp)
                    .height(73.dp)
                    .offset(x = xOff),
            )
        }
    }
}
