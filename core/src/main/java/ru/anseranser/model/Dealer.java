package ru.anseranser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the 36-card deck, shuffles it with an injected {@link Random}, and deals
 * it round-robin to the active (still-in-game) players.
 *
 * <p>Extracted from {@code Game} in refactor Stage 4 so shuffling/dealing is
 * isolated and testable with a seeded RNG (see {@code DealerTest}).
 */
public class Dealer {

    private final List<Card> deck = new ArrayList<>();

    public Dealer() {
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
    }

    public List<Card> deck() {
        return deck;
    }

    /** Shuffle the deck in place using the supplied RNG. */
    public void shuffle(Random rng) {
        java.util.Collections.shuffle(deck, rng);
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
            current.getHand().add(card);
            current = order.nextActive(current, active);
        }
        deck.clear();
    }
}
