package ru.anseranser.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.BackgroundDark
import ru.anseranser.android.ui.theme.TextGold
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

@Composable
fun PlayerHandView(
    hand: List<Card>,
    validChoices: List<Card>,
    isHumanTurn: Boolean,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = hand.sortedWith(
        compareBy<Card> { it.suit.ordinal }.thenBy { it.rank.value }
    )
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(BackgroundDark)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR HAND (${sorted.size})",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = TextGold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sorted, key = { "${it.suit}_${it.rank}" }) { card ->
                AnimatedVisibility(
                    visible = true,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CardView(
                        card = card,
                        isPlayable = isHumanTurn && validChoices.contains(card),
                        isSelected = false,
                        onClick = if (isHumanTurn && validChoices.contains(card)) {
                            { onCardClick(card) }
                        } else null
                    )
                }
            }
        }
    }
}
