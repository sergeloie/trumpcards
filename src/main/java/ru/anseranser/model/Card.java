package ru.anseranser.model;

import lombok.Getter;

public record Card(Suit suit, Rank rank) {
    public enum Suit {
        SPADES('\u2660'), CLUBS('\u2663'), DIAMONDS('\u2666'), HEARTS('\u2665');

        private final char symbol;

        Suit(char symbol) {
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return String.valueOf(symbol);
        }
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

    @Override
    public String toString() {
        return rank + "" + suit.getSymbol();
    }
}
