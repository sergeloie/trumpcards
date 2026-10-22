package ru.anseranser.trumpcards.util

import ru.anseranser.engine.Card

/**
 * The names of the pictures of the cards in the assets of the app.
 *
 * <p>The rank of the engine has no value of its own, so the number in the name
 * of the picture is the ordinal of the rank, counted from the deuce.</p>
 */
object CardImageMapper {

    private val suitToAsset = mapOf(
        Card.Suit.SPADES to "spade",
        Card.Suit.HEARTS to "heart",
        Card.Suit.DIAMONDS to "diamond",
        Card.Suit.CLUBS to "club",
    )

    fun facePath(card: Card): String {
        val suitName = suitToAsset[card.suit()] ?: error("Unknown suit: ${card.suit()}")
        return "cards/faces/${suitName}_${rankNumber(card.rank())}.png"
    }

    fun backPath(): String = "cards/backs/card_back_0.png"

    private fun rankNumber(rank: Card.Rank): Int = rank.ordinal + 2
}
