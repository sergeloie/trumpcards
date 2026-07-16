package ru.anseranser.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Per-suit "scoreboard" stacks (доска счёта): each suit holds a deque of cards.
 *
 * <p>Extracted from {@code Game} in refactor Stage 4 so the scoring/elimination
 * rules are isolated and unit-testable in isolation (see {@code ScoreboardTest}).
 * The class is pure data + rules — no I/O, no player references.
 */
public class Scoreboard {

    private final Map<Card.Suit, Deque<Card>> stacks;

    public Scoreboard() {
        stacks = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            stacks.put(suit, new ArrayDeque<>());
        }
    }

    /** Extract the SIX of each suit from the deck into its stack. Call once, after the deck is built. */
    public void init(List<Card> deck) {
        for (Card.Suit suit : Card.Suit.values()) {
            stacks.get(suit).push(extract(deck, suit, Card.Rank.SIX));
        }
    }

    private static Card extract(List<Card> deck, Card.Suit suit, Card.Rank rank) {
        Iterator<Card> it = deck.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.suit() == suit && c.rank() == rank) {
                it.remove();
                return c;
            }
        }
        throw new IllegalStateException("Card " + rank + " of " + suit + " not found in the deck");
    }

    /**
     * The next rank that must be passed to the owner of {@code suit}, or {@code null}
     * once the ladder reaches the top (no further obligatory exchange).
     */
    public Card.Rank nextRequiredRank(Card.Suit suit) {
        Deque<Card> stack = stacks.get(suit);
        if (stack.isEmpty()) return null;
        Card.Rank lastRemoved = stack.peek().rank();
        Card.Rank[] ranks = Card.Rank.values();
        int nextOrdinal = lastRemoved.ordinal() + 1;
        return nextOrdinal >= ranks.length ? null : ranks[nextOrdinal];
    }

    /**
     * Push a card onto its suit's stack.
     *
     * @return {@code true} if pushing this card eliminates the owner
     *         (i.e. an ACE was pushed — the ladder is complete).
     */
    public boolean pushAndEliminates(Card card) {
        stacks.get(card.suit()).push(card);
        return card.rank() == Card.Rank.ACE;
    }

    /** Top-first snapshot of every stack, for events. */
    public Map<Card.Suit, List<Card>> snapshot() {
        Map<Card.Suit, List<Card>> snapshot = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            snapshot.put(suit, new ArrayList<>(stacks.get(suit)));
        }
        return snapshot;
    }

    /**
     * Restore the stacks from a previously taken {@link #snapshot()} (R10). The
     * supplied lists are top-first, so they are pushed in reverse to preserve the
     * exact top-to-bottom order. Used when loading a saved game.
     */
    void restore(Map<Card.Suit, List<Card>> saved) {
        for (Card.Suit suit : Card.Suit.values()) {
            Deque<Card> stack = stacks.get(suit);
            stack.clear();
            List<Card> cards = saved.get(suit);
            if (cards != null) {
                for (int i = cards.size() - 1; i >= 0; i--) {
                    stack.push(cards.get(i));
                }
            }
        }
    }
}
