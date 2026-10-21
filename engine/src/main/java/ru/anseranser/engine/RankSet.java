package ru.anseranser.engine;

import java.util.List;

/**
 * The ranks a deck is built from. The lowest rank of the set is the base rank.
 */
public enum RankSet {

    /** The classic 36-card rank set: SIX to ACE. */
    SIX_TO_ACE(Card.Rank.SIX),

    /** The 52-card rank set: TWO to ACE. */
    TWO_TO_ACE(Card.Rank.TWO);

    private final Card.Rank baseRank;

    RankSet(Card.Rank baseRank) {
        this.baseRank = baseRank;
    }

    /** The lowest rank of the set, and so the base card of every ladder. */
    public Card.Rank baseRank() {
        return baseRank;
    }

    /** The ranks of the set, ascending from the base rank to the ace. */
    public List<Card.Rank> ranks() {
        Card.Rank[] all = Card.Rank.values();
        return List.of(all).subList(baseRank.ordinal(), all.length);
    }

    /** The number of cards in a game of {@code seats} seats, base cards included. */
    public int cardCount(int seats) {
        return ranks().size() * seats;
    }
}
