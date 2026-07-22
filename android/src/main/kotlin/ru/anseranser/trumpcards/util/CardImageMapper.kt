package ru.anseranser.trumpcards.util

import ru.anseranser.model.Card

object CardImageMapper {

    private val suitToAsset = mapOf(
        Card.Suit.SPADES to "spade",
        Card.Suit.HEARTS to "heart",
        Card.Suit.DIAMONDS to "diamond",
        Card.Suit.CLUBS to "club",
    )

    fun facePath(card: Card): String {
        val suitName = suitToAsset[card.suit()] ?: error("Unknown suit: ${card.suit()}")
        val rankValue = card.rank().value
        return "cards/faces/${suitName}_${rankValue}.png"
    }

    fun backPath(): String = "cards/backs/card_back_0.png"

    fun backgroundPath(): String = "backgrounds/background_0.png"
}
