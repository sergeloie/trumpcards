package ru.anseranser.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Card {
    public enum Suit {
        SPADES, CLUBS, DIAMONDS, HEARTS
    }

    @Getter
    public enum Rank {
        SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10),
        JACK(11), QUEEN(12), KING(13), ACE(14);

        private final int value;

        Rank(int value) {
            this.value = value;
        }

    }

    private final Suit suit;
    private final Rank rank;

    @Override
    public String toString() {
        return rank + " of " + suit;
    }

}
