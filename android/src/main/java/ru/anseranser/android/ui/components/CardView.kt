package ru.anseranser.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.anseranser.model.Card

@Composable
fun CardView(
    card: Card,
    isPlayable: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resId = cardToResId(card)
    val elevation by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        label = "card_elevation"
    )

    val borderModifier = if (isPlayable && !isSelected) {
        Modifier.border(2.dp, Color(0xFF66BB6A), RoundedCornerShape(6.dp))
    } else if (isSelected) {
        Modifier.border(3.dp, Color(0xFFFFD54F), RoundedCornerShape(6.dp))
    } else {
        Modifier
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = "${card.rank} of ${card.suit}",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(CARD_WIDTH)
            .height(CARD_HEIGHT)
            .offset(y = elevation)
            .clip(RoundedCornerShape(6.dp))
            .then(borderModifier)
            .then(
                if (onClick != null && isPlayable) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    )
}

@Composable
fun CardBack(
    suit: Card.Suit,
    modifier: Modifier = Modifier
) {
    val resId = when (suit) {
        Card.Suit.SPADES -> cardBackResId("blue")
        Card.Suit.CLUBS -> cardBackResId("green")
        Card.Suit.DIAMONDS -> cardBackResId("red")
        Card.Suit.HEARTS -> cardBackResId("red")
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = "Card back",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun cardBackResId(color: String): Int {
    val context = LocalContext.current
    return context.resources.getIdentifier("card_back_$color", "drawable", context.packageName)
}

@Composable
private fun cardToResId(card: Card): Int {
    val suitWord = when (card.suit) {
        Card.Suit.SPADES -> "spade"
        Card.Suit.CLUBS -> "club"
        Card.Suit.DIAMONDS -> "diamond"
        Card.Suit.HEARTS -> "heart"
    }
    val rankCode = when (card.rank) {
        Card.Rank.SIX -> "6"
        Card.Rank.SEVEN -> "7"
        Card.Rank.EIGHT -> "8"
        Card.Rank.NINE -> "9"
        Card.Rank.TEN -> "10"
        Card.Rank.JACK -> "j"
        Card.Rank.QUEEN -> "q"
        Card.Rank.KING -> "k"
        Card.Rank.ACE -> "a"
    }
    val resName = "card_${suitWord}_$rankCode"
    val context = LocalContext.current
    return context.resources.getIdentifier(resName, "drawable", context.packageName)
}

val CARD_WIDTH = 70.dp
val CARD_HEIGHT = 96.dp
