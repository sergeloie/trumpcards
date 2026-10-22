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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anseranser.engine.Card
import ru.anseranser.trumpcards.R

@Composable
fun PotArea(
    pot: List<Card>,
    modifier: Modifier = Modifier,
) {
    if (pot.isEmpty()) return

    val visible = if (pot.size > 5) pot.takeLast(5) else pot

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pluralStringResource(R.plurals.pot_cards, pot.size, pot.size),
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
