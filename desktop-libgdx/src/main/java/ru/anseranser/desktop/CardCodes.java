package ru.anseranser.desktop;

import ru.anseranser.model.Card;

/**
 * The names of the cards the desktop shows: the letter of the rank and the
 * letter of the suit, in plain ASCII, so that every font can draw them.
 */
final class CardCodes {

    private CardCodes() {
    }

    /** The name of a card, e.g. "AS" or "7H". */
    static String of(Card card) {
        return rankLetter(card.rank()) + suitLetter(card.suit());
    }

    /** The letter of a rank. */
    static String rankLetter(Card.Rank rank) {
        return switch (rank) {
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
            case ACE -> "A";
        };
    }

    /** The letter of a suit. */
    static String suitLetter(Card.Suit suit) {
        return switch (suit) {
            case SPADES -> "S";
            case HEARTS -> "H";
            case DIAMONDS -> "D";
            case CLUBS -> "C";
        };
    }
}
