package ru.anseranser.model;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Supplies the cards dealt each round. This is the seam that decouples
 * {@link Game} from the concrete {@link Dealer}: production games use the
 * RNG-driven {@code Dealer}, while tests can inject a deterministic
 * {@code ScriptedCardDealer} that places an explicit layout into the hands
 * without touching the RNG at all.
 *
 * <p>The dealer owns the <em>card pool</em> — the cards not currently on a hand,
 * on the scoreboard, or in the pot. Between rounds the pool is refilled with the
 * loser's hand plus the pot ({@link #returnCards(Collection)}), then shuffled and
 * dealt again. A dealer must therefore be stateful across a game.</p>
 *
 * <p>Implementations must be deterministic for a given pool + RNG state. The
 * read-only {@link #deck()} view follows the same immutability contract (R5) as
 * the other domain snapshots, so callers can never mutate the pool through it.</p>
 */
public interface CardDealer {

    /**
     * Read-only view of the card pool awaiting the next deal. Empty once a deal
     * has emptied it and before the round-end cards are returned.
     */
    List<Card> deck();

    /**
     * Remove and return the four base-rank cards (one per suit) that seed the
     * scoreboard ladders. Called once during game setup, before the first deal.
     *
     * @param baseRank the deck's lowest rank (see {@link DeckSize#baseRank()})
     */
    List<Card> takeBaseCards(Card.Rank baseRank);

    /** Shuffle the pool in place using the supplied RNG (production path). */
    void shuffle(Rng rng);

    /**
     * Deal the entire pool to the active players, starting at the player to the
     * left of {@code dealer}. Only players satisfying {@code active} receive
     * cards; the pool is emptied.
     */
    void deal(TurnOrder order, Player dealer, Predicate<Player> active);

    /** Return cards to the pool (called at the end of a round). */
    void returnCards(Collection<Card> cards);

    /** Replace the pool contents (used when restoring a saved game). */
    void setDeck(Collection<Card> cards);
}
