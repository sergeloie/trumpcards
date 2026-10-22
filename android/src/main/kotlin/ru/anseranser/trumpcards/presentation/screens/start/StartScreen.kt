package ru.anseranser.trumpcards.presentation.screens.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.anseranser.engine.RankSet
import ru.anseranser.trumpcards.R

/** The seats at the table: one per suit in play. */
private const val SEATS = 4

@Composable
fun StartScreen(
    rememberedRankSet: RankSet,
    onStartGame: (RankSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(R.string.choose_deck),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(32.dp))

            DeckButton(
                rankSet = RankSet.SIX_TO_ACE,
                remembered = rememberedRankSet == RankSet.SIX_TO_ACE,
                onClick = { onStartGame(RankSet.SIX_TO_ACE) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            DeckButton(
                rankSet = RankSet.TWO_TO_ACE,
                remembered = rememberedRankSet == RankSet.TWO_TO_ACE,
                onClick = { onStartGame(RankSet.TWO_TO_ACE) },
            )
        }
    }
}

@Composable
private fun DeckButton(
    rankSet: RankSet,
    remembered: Boolean,
    onClick: () -> Unit,
) {
    val cards = rankSet.cardCount(SEATS)
    val label = pluralStringResource(R.plurals.deck_cards, cards, cards)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (remembered) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
        ),
    ) {
        Text(
            text = if (remembered) "✓ $label" else label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
