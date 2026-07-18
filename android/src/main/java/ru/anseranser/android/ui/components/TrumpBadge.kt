package ru.anseranser.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.GoldAccent
import ru.anseranser.android.ui.theme.TableGreenLight
import ru.anseranser.model.Card

@Composable
fun TrumpBadge(
    trump: Card.Suit,
    modifier: Modifier = Modifier
) {
    Text(
        text = "TRUMP: ${trump.name}",
        style = MaterialTheme.typography.titleMedium,
        color = GoldAccent,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TableGreenLight.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
