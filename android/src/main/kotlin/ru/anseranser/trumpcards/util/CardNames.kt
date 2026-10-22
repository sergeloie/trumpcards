package ru.anseranser.trumpcards.util

import ru.anseranser.engine.Card
import ru.anseranser.trumpcards.R

/** The name of a suit, as the app writes it. */
fun suitNameRes(suit: Card.Suit): Int = when (suit) {
    Card.Suit.SPADES -> R.string.suit_spades
    Card.Suit.CLUBS -> R.string.suit_clubs
    Card.Suit.DIAMONDS -> R.string.suit_diamonds
    Card.Suit.HEARTS -> R.string.suit_hearts
}

/** The name of a suit in a card, as the app writes it. */
fun cardSuitNameRes(suit: Card.Suit): Int = when (suit) {
    Card.Suit.SPADES -> R.string.card_suit_spades
    Card.Suit.CLUBS -> R.string.card_suit_clubs
    Card.Suit.DIAMONDS -> R.string.card_suit_diamonds
    Card.Suit.HEARTS -> R.string.card_suit_hearts
}

/** The name of a rank, as the app writes it. */
fun rankNameRes(rank: Card.Rank): Int = when (rank) {
    Card.Rank.TWO -> R.string.rank_two
    Card.Rank.THREE -> R.string.rank_three
    Card.Rank.FOUR -> R.string.rank_four
    Card.Rank.FIVE -> R.string.rank_five
    Card.Rank.SIX -> R.string.rank_six
    Card.Rank.SEVEN -> R.string.rank_seven
    Card.Rank.EIGHT -> R.string.rank_eight
    Card.Rank.NINE -> R.string.rank_nine
    Card.Rank.TEN -> R.string.rank_ten
    Card.Rank.JACK -> R.string.rank_jack
    Card.Rank.QUEEN -> R.string.rank_queen
    Card.Rank.KING -> R.string.rank_king
    Card.Rank.ACE -> R.string.rank_ace
}
