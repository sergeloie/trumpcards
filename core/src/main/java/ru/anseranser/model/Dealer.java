package ru.anseranser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a deck of the configured {@link DeckSize}, shuffles it with an injected
 * {@link Rng}, and deals it round-robin to the active (still-in-game) players.
 *
 * <p>Extracted from {@code Game} in refactor Stage 4 so shuffling/dealing is
 * isolated and testable with a seeded RNG (see {@code DealerTest}). The deck
 * contents come from {@link DeckSize#ranks()} so the same dealer serves both the
 * 36- and 52-card variants.</p>
 */
public class Dealer {

    private final List<Card> deck = new ArrayList<>();

    /** Build the default 36-card deck. */
    public Dealer() {
        this(DeckSize.THIRTY_SIX);
    }

    /** Build the deck for the given {@link DeckSize}. */
    public Dealer(DeckSize deckSize) {
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : deckSize.ranks()) {
                deck.add(new Card(suit, rank));
            }
        }
    }

    public List<Card> deck() {
        return deck;
    }

    /** Shuffle the deck in place using the supplied RNG (Fisher-Yates). */
    public void shuffle(Rng rng) {
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }

    /**
     * Deal all cards of the shuffled deck to the active players, starting left of
     * {@code dealer}. Only players satisfying {@code active} receive cards; the
     * deck is emptied.
     *
     * @param order  seating order around the table
     * @param dealer the current dealer; dealing starts at the player to their left
     * @param active predicate selecting who still receives cards (e.g. {@code Player::isGamer})
     */
    public void deal(TurnOrder order, Player dealer, java.util.function.Predicate<Player> active) {
        Player current = order.nextActive(dealer, active);
        for (Card card : deck) {
            current.addCard(card);
            current = order.nextActive(current, active);
        }
        deck.clear();
    }
}
