package ru.anseranser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Deterministic, RNG-free {@link CardDealer} for tests. Instead of shuffling a
 * deck it places an <em>explicit layout</em> into the players' hands, so a test
 * can assert exact card positions without re-implementing {@link SplitMix64} or
 * guessing a seed.
 *
 * <p>Constructed from a map keyed by each player's trump suit, whose values are
 * that player's hand after dealing. The layout is validated eagerly:</p>
 * <ul>
 *   <li>no card appears twice;</li>
 *   <li>every card belongs to the configured {@link DeckSize} (its rank is dealt)
 *       and is <em>not</em> a scoreboard base card;</li>
 *   <li>the total number of cards equals {@code deckSize.cardCount() - 4}
 *       (the deck minus the four base cards that seed the scoreboard ladders).</li>
 * </ul>
 *
 * <p>The first {@link #deal} call applies the script. A scripted layout only
 * describes the opening deal; if the same dealer is reused for later rounds the
 * remaining pool is dealt deterministically in seating order (no shuffle), which
 * keeps a full scripted game card-conserving.</p>
 */
public final class ScriptedCardDealer implements CardDealer {

    private final DeckSize deckSize;
    private final Map<Card.Suit, List<Card>> handsByTrump;
    private final List<Card> pool = new ArrayList<>();
    private boolean scriptDealt = false;

    /**
     * @param deckSize     the deck configuration the layout must match
     * @param handsByTrump explicit post-deal hands, keyed by the player's trump suit
     * @throws IllegalArgumentException if the layout violates a dealing invariant
     */
    public ScriptedCardDealer(DeckSize deckSize, Map<Card.Suit, List<Card>> handsByTrump) {
        this.deckSize = Objects.requireNonNull(deckSize, "deckSize");
        Objects.requireNonNull(handsByTrump, "handsByTrump");

        Map<Card.Suit, List<Card>> copy = new EnumMap<>(Card.Suit.class);
        for (Map.Entry<Card.Suit, List<Card>> e : handsByTrump.entrySet()) {
            copy.put(Objects.requireNonNull(e.getKey(), "hand suit"),
                    List.copyOf(Objects.requireNonNull(e.getValue(), "hand for " + e.getKey())));
        }
        validate(copy);
        this.handsByTrump = Collections.unmodifiableMap(copy);
    }

    private void validate(Map<Card.Suit, List<Card>> hands) {
        List<Card> all = new ArrayList<>();
        Set<Card> seen = new HashSet<>();
        for (Map.Entry<Card.Suit, List<Card>> e : hands.entrySet()) {
            for (Card card : e.getValue()) {
                if (card.rank() == deckSize.baseRank()) {
                    throw new IllegalArgumentException(
                            "Scripted layout contains " + card + ", which must be on the scoreboard, not in a hand");
                }
                if (!deckSize.ranks().contains(card.rank())) {
                    throw new IllegalArgumentException(
                            "Card " + card + " is not part of a " + deckSize + " deck");
                }
                if (!seen.add(card)) {
                    throw new IllegalArgumentException("Duplicate card in scripted layout: " + card);
                }
                all.add(card);
            }
        }
        int expected = deckSize.cardCount() - Card.Suit.values().length;
        if (all.size() != expected) {
            throw new IllegalArgumentException("Scripted layout has " + all.size()
                    + " cards, expected " + expected
                    + " (" + deckSize.cardCount() + " minus one base card per suit)");
        }
    }

    @Override
    public List<Card> deck() {
        return Collections.unmodifiableList(pool);
    }

    @Override
    public List<Card> takeBaseCards(Card.Rank baseRank) {
        if (baseRank != deckSize.baseRank()) {
            throw new IllegalArgumentException(
                    "Expected base rank " + deckSize.baseRank() + " for " + deckSize + ", got " + baseRank);
        }
        List<Card> base = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            base.add(new Card(suit, baseRank));
        }
        return base;
    }

    @Override
    public void shuffle(Rng rng) {
        // The scripted layout is fixed; the RNG is intentionally ignored.
    }

    @Override
    public void deal(TurnOrder order, Player dealer, Predicate<Player> active) {
        if (!scriptDealt) {
            dealScript(order, active);
            scriptDealt = true;
            return;
        }
        dealPool(order, dealer, active);
    }

    private void dealScript(TurnOrder order, Predicate<Player> active) {
        List<Player> activePlayers = new ArrayList<>();
        for (Player p : order) {
            if (active.test(p)) {
                activePlayers.add(p);
            }
        }
        // Give every active player their scripted hand.
        for (Player p : activePlayers) {
            List<Card> hand = handsByTrump.get(p.getTrump());
            if (hand == null) {
                throw new IllegalArgumentException("No scripted hand for player with trump " + p.getTrump());
            }
            for (Card card : hand) {
                p.addCard(card);
            }
        }
        // A non-empty scripted hand for a suit not at the table would silently lose
        // those cards, so reject it. Empty extra entries (e.g. from a builder that
        // pre-populates all four suits) are harmless and ignored.
        for (Map.Entry<Card.Suit, List<Card>> entry : handsByTrump.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            boolean owned = false;
            for (Player p : activePlayers) {
                if (p.getTrump() == entry.getKey()) {
                    owned = true;
                    break;
                }
            }
            if (!owned) {
                throw new IllegalArgumentException("Scripted hand for " + entry.getKey()
                        + " but no such player is active");
            }
        }
    }

    private void dealPool(TurnOrder order, Player dealer, Predicate<Player> active) {
        Player current = order.nextActive(dealer, active);
        for (Card card : pool) {
            current.addCard(card);
            current = order.nextActive(current, active);
        }
        pool.clear();
    }

    @Override
    public void returnCards(Collection<Card> cards) {
        pool.addAll(cards);
    }

    @Override
    public void setDeck(Collection<Card> cards) {
        pool.clear();
        pool.addAll(cards);
    }

    // ---------- Layout builder ----------

    /**
     * Start building a layout for the given deck size. The builder lets a test
     * pin a few cards to specific hands and auto-fill the rest, instead of
     * spelling out all {@code deckSize.cardCount() - 4} cards by hand.
     */
    public static Builder builder(DeckSize deckSize) {
        return new Builder(deckSize);
    }

    /** A dealer whose layout is the whole non-scoreboard deck dealt round-robin in suit order. */
    public static ScriptedCardDealer full(DeckSize deckSize) {
        return builder(deckSize).buildDealer();
    }

    /** A dealer whose layout is the whole deck dealt round-robin in the given player order. */
    public static ScriptedCardDealer full(DeckSize deckSize, Card.Suit... fillOrder) {
        return builder(deckSize).fillOrder(fillOrder).buildDealer();
    }

    /**
     * Fluent builder for a {@link ScriptedCardDealer} layout.
     *
     * <pre>{@code
     * ScriptedCardDealer dealer = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
     *         .give(Card.Suit.SPADES, new Card(SPADES, SEVEN), new Card(CLUBS, NINE))
     *         .give(Card.Suit.HEARTS, new Card(HEARTS, ACE))
     *         .buildDealer();   // the remaining cards are dealt round-robin
     * }</pre>
     *
     * <p>Explicit cards are checked eagerly (distinct, in-deck, not a scoreboard
     * base card). {@link #build()} auto-fills any cards left unassigned, so the
     * layout always covers the deck minus the four base cards.</p>
     */
    public static final class Builder {

        private final DeckSize deckSize;
        private final Map<Card.Suit, List<Card>> hands = new EnumMap<>(Card.Suit.class);
        private final Set<Card> used = new HashSet<>();
        private Card.Suit[] fillOrder = Card.Suit.values();
        private boolean filled = false;

        private Builder(DeckSize deckSize) {
            this.deckSize = Objects.requireNonNull(deckSize, "deckSize");
            for (Card.Suit suit : Card.Suit.values()) {
                hands.put(suit, new ArrayList<>());
            }
        }

        /**
         * Set the player order used when the remaining cards are auto-filled
         * (by {@link #fill()} or {@link #build()}). Use this for golden layouts
         * that need the leftover cards to land in a specific sequence.
         */
        public Builder fillOrder(Card.Suit... order) {
            this.fillOrder = requireOrder(order);
            return this;
        }

        /** Pin the given cards to one player's hand. */
        public Builder give(Card.Suit player, Card... cards) {
            return give(player, List.of(cards));
        }

        /** Pin the given cards to one player's hand. */
        public Builder give(Card.Suit player, Collection<Card> cards) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(cards, "cards");
            for (Card card : cards) {
                if (card.rank() == deckSize.baseRank()) {
                    throw new IllegalArgumentException(
                            "Card " + card + " must be on the scoreboard, not in a hand");
                }
                if (!deckSize.ranks().contains(card.rank())) {
                    throw new IllegalArgumentException(
                            "Card " + card + " is not part of a " + deckSize + " deck");
                }
                if (!used.add(card)) {
                    throw new IllegalArgumentException("Duplicate card in scripted layout: " + card);
                }
                hands.get(player).add(card);
            }
            return this;
        }

        /** Deal every still-unassigned card round-robin, in the configured fill order. */
        public Builder fill() {
            return fillWith(fillOrder);
        }

        /** Deal every still-unassigned card round-robin, cycling through {@code order}. */
        public Builder fill(Card.Suit... order) {
            return fillWith(requireOrder(order));
        }

        private Builder fillWith(Card.Suit[] order) {
            if (filled) {
                return this;
            }
            int i = 0;
            for (Card.Suit suit : Card.Suit.values()) {
                for (Card.Rank rank : deckSize.ranks()) {
                    if (rank == deckSize.baseRank()) {
                        continue;
                    }
                    Card card = new Card(suit, rank);
                    if (used.add(card)) {
                        hands.get(order[i++ % order.length]).add(card);
                    }
                }
            }
            filled = true;
            return this;
        }

        private static Card.Suit[] requireOrder(Card.Suit[] order) {
            Objects.requireNonNull(order, "order");
            if (order.length == 0) {
                throw new IllegalArgumentException("fill order must not be empty");
            }
            return order.clone();
        }

        /** Complete and return the immutable layout (auto-filling if needed). */
        public Map<Card.Suit, List<Card>> build() {
            fill();
            Map<Card.Suit, List<Card>> copy = new EnumMap<>(Card.Suit.class);
            for (Card.Suit suit : Card.Suit.values()) {
                copy.put(suit, List.copyOf(hands.get(suit)));
            }
            return Collections.unmodifiableMap(copy);
        }

        /** Complete the layout and wrap it in a ready-to-use {@link ScriptedCardDealer}. */
        public ScriptedCardDealer buildDealer() {
            return new ScriptedCardDealer(deckSize, build());
        }
    }
}
