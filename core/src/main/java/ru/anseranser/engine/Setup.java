package ru.anseranser.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * How a game is set up: the rank set, the trump suits, the seed of the random
 * generator and, optionally, the order of the deck.
 *
 * <p>Every suit in play belongs to exactly one seat, so the trump suits are the
 * suits of the table and must be distinct.
 *
 * <p>An explicit deck order replaces the deck the engine builds and shuffles:
 * the cards are dealt in the order they are given, and the generator is only
 * asked for the dealer. It is empty in an ordinary game.
 */
public record Setup(RankSet rankSet, List<Card.Suit> trumpSuits, long seed, List<Card> deckOrder) {

    /** A game whose deck the engine builds and shuffles itself. */
    public Setup(RankSet rankSet, List<Card.Suit> trumpSuits, long seed) {
        this(rankSet, trumpSuits, seed, List.of());
    }

    public Setup {
        Objects.requireNonNull(rankSet, "rankSet");
        trumpSuits = List.copyOf(trumpSuits);
        if (trumpSuits.size() < 2 || trumpSuits.size() > 4) {
            throw new IllegalArgumentException(
                    "a game is played by 2 to 4 seats, got trump suits " + trumpSuits);
        }
        if (new HashSet<>(trumpSuits).size() != trumpSuits.size()) {
            throw new IllegalArgumentException("trump suits must be distinct, got " + trumpSuits);
        }
        deckOrder = List.copyOf(deckOrder);
        if (!deckOrder.isEmpty()) {
            checkDeckOrder(rankSet, trumpSuits, deckOrder);
        }
    }

    private static void checkDeckOrder(RankSet rankSet, List<Card.Suit> trumpSuits, List<Card> deckOrder) {
        int expected = rankSet.cardCount(trumpSuits.size()) - trumpSuits.size();
        if (deckOrder.size() != expected) {
            throw new IllegalArgumentException(
                    "the deck order holds " + deckOrder.size() + " cards, expected " + expected);
        }
        Set<Card> distinct = new HashSet<>(deckOrder);
        if (distinct.size() != deckOrder.size()) {
            throw new IllegalArgumentException("the deck order holds a card twice");
        }
        for (Card card : deckOrder) {
            if (!trumpSuits.contains(card.suit())) {
                throw new IllegalArgumentException(card + " is not a suit in play");
            }
            if (card.rank() == rankSet.baseRank()) {
                throw new IllegalArgumentException(card + " belongs on a board, not in the deck");
            }
            if (!rankSet.ranks().contains(card.rank())) {
                throw new IllegalArgumentException(card + " is not a rank of the rank set in play");
            }
        }
    }
}
