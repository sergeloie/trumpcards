package ru.anseranser.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.TableGreen
import ru.anseranser.android.ui.theme.TextGold
import ru.anseranser.android.ui.theme.TextWhite
import ru.anseranser.model.Card

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PotView(
    pot: List<Card>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TableGreen)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "POT (${pot.size})",
            style = MaterialTheme.typography.labelSmall,
            color = TextGold
        )

        if (pot.isEmpty()) {
            Text(
                text = "Empty",
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy((-4).dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                pot.forEach { card ->
                    CardView(
                        card = card,
                        isPlayable = false,
                        isSelected = false
                    )
                }
            }
        }
    }
}
