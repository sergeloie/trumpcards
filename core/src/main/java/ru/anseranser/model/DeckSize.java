package ru.anseranser.model;

import java.util.Arrays;
import java.util.List;

/**
 * The two supported deck configurations. A deck size determines two things that
 * differ between the 36- and 52-card variants:
 * <ul>
 *   <li>which {@link Card.Rank}s are dealt (the deck contents), and</li>
 *   <li>the lowest rank — the card seeded at the base of every scoreboard ladder
 *       ({@link #baseRank()}). For a 36-card deck that is SIX; for a 52-card deck
 *       it is TWO, the new lowest rank.</li>
 * </ul>
 *
 * <p>The scoreboard ladder advances by {@code ordinal() + 1}, so once the base
 * rank is set the ladder automatically runs base..ACE with no extra rule code.
 * This enum is the single source of truth that {@link Dealer}, {@link Scoreboard},
 * {@link Game}, {@link SavedGame} and {@link GameSimulator} all read from.</p>
 */
public enum DeckSize {

    /** Traditional 36-card deck: ranks SIX..ACE, scoreboard base SIX. */
    THIRTY_SIX(36, Card.Rank.SIX),

    /** Full 52-card deck: ranks TWO..ACE, scoreboard base TWO. */
    FIFTY_TWO(52, Card.Rank.TWO);

    private final int cardCount;
    private final Card.Rank baseRank;

    DeckSize(int cardCount, Card.Rank baseRank) {
        this.cardCount = cardCount;
        this.baseRank = baseRank;
    }

    /** Total number of cards in this deck (4 suits × ranks). */
    public int cardCount() {
        return cardCount;
    }

    /**
     * The lowest rank in this deck — the card placed at the base of each
     * scoreboard ladder (SIX for 36 cards, TWO for 52 cards).
     */
    public Card.Rank baseRank() {
        return baseRank;
    }

    /** Ranks included in this deck, in ascending order (baseRank .. ACE). */
    public List<Card.Rank> ranks() {
        Card.Rank[] all = Card.Rank.values();
        return Arrays.asList(all).subList(baseRank.ordinal(), all.length);
    }
}
