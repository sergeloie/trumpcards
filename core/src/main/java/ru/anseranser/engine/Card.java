package ru.anseranser.engine;

/**
 * A card of the game: one suit and one rank.
 *
 * <p>The suits are declared in the canonical order the deck is built in.
 */
public record Card(Suit suit, Rank rank) {

    /** The suits of the game, in the canonical order of the deck. */
    public enum Suit {
        SPADES, CLUBS, DIAMONDS, HEARTS
    }

    /** The ranks a deck is built from, declared in ascending order. */
    public enum Rank {
        TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
