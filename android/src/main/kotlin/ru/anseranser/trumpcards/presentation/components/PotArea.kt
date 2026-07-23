package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
fun PotArea(
    pot: List<Card>,
    modifier: Modifier = Modifier,
) {
    val cards = pot.filterNotNull()
    if (cards.isEmpty()) return

    val visible = if (cards.size > 5) cards.takeLast(5) else cards

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${cards.size} карт",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            visible.forEach { card ->
                CardView(
                    card = card,
                    modifier = Modifier
                        .width(52.dp)
                        .height(73.dp),
                )
            }
        }
    }
}
