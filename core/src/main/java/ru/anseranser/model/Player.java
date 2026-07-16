package ru.anseranser.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class Player {

    private final Card.Suit trump;
    @Getter(AccessLevel.NONE)
    private final List<Card> hand = new ArrayList<>();
    @Setter
    private boolean gamer = true;
    @Setter
    private DecisionStrategy strategy = new AiDecisionStrategy();

    public Player(Card.Suit trump, DecisionStrategy strategy) {
        this.trump = trump;
        this.strategy = strategy;
    }

    /**
     * Unmodifiable view of this player's hand. External code (UI, input
     * providers, strategies) may read but never mutate the hand through this
     * getter — the engine mutates the backing list internally (R5).
     */
    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    // --- Engine-internal mutators (same package only) ---
    // The public getHand() is unmodifiable (R5); the engine mutates the hand
    // through these explicit methods so external code can never corrupt state.

    void addCard(Card card) {
        hand.add(card);
    }

    boolean removeCard(Card card) {
        return hand.remove(card);
    }

    void clearHand() {
        hand.clear();
    }

    /**
     * Whether this seat is controlled by a human. Delegated to the
     * {@link DecisionStrategy} so the domain carries no presentation concern.
     */
    public boolean isHuman() {
        return strategy.isHuman();
    }

    protected boolean canBeat(Card attacking, Card defending) {
        if (attacking.suit() == defending.suit()) {
            return attacking.rank().getValue() < defending.rank().getValue();
        } else return defending.suit() == trump;
    }

    /** All cards in hand that legally beat {@code attacking}, weakest first. */
    protected List<Card> validDefenses(Card attacking) {
        return hand.stream()
                .filter(c -> canBeat(attacking, c))
                .sorted(Comparator
                        .comparing((Card c) -> c.suit() == trump) // non-trumps first (false < true)
                        .thenComparing(c -> c.rank().getValue()))
                .toList();
    }

    protected void collectPot(List<Card> pot) {
        hand.addAll(pot);
        pot.clear();
    }

    @Override
    public String toString() {
        return "Player(trump=" + trump + ", cards=" + hand.size() + ")";
    }

    public Card chooseLeadCard() {
        return strategy.chooseLeadCard(this, getHand());
    }

    /**
     * Play this player's turn. The method is pure with respect to the output
     * layer: it mutates only this player's hand and the shared {@code pot}, and
     * returns the ordered list of domain actions that occurred (lead / beat /
     * take-pot). The engine translates those actions into {@link
     * ru.anseranser.event.GameEvent}s — the player never references the event
     * system, keeping the domain model free of presentation concerns.
     */
    public List<MoveAction> makeMove(List<Card> pot) {
        List<MoveAction> actions = new ArrayList<>();
        if (hand.isEmpty()) return actions;

        if (pot.isEmpty()) {
            actions.add(playLeadCard(pot));
            return actions;
        }

        Card topCard = pot.getLast();
        List<Card> defenses = validDefenses(topCard);

        if (defenses.isEmpty()) {
            actions.add(takePot(topCard, pot));
            return actions;
        }

        Card chosen = strategy.chooseDefenseCard(this, topCard, defenses);
        actions.add(beatCard(topCard, chosen, pot));

        if (hand.isEmpty()) {
            return actions;
        }

        actions.add(playLeadCard(pot));
        return actions;
    }

    protected MoveAction playLeadCard(List<Card> pot) {
        return playLeadCard(pot, null);
    }

    /**
     * Lead a card into the pot. When {@code forced} is non-null it is used as the
     * led card (resume/async input path, where the human's choice was supplied
     * out-of-band); otherwise the {@link DecisionStrategy} decides. The forced
     * card must currently be in this player's hand.
     */
    protected MoveAction playLeadCard(List<Card> pot, Card forced) {
        Card leadCard = forced != null ? forced : chooseLeadCard();
        if (forced != null && !hand.contains(forced)) {
            throw new IllegalArgumentException("forced lead card not in hand: " + forced);
        }
        pot.add(leadCard);
        hand.remove(leadCard);
        return new MoveAction.Led(leadCard);
    }

    protected MoveAction takePot(Card topCard, List<Card> pot) {
        int taken = pot.size();
        collectPot(pot);
        return new MoveAction.TookPot(topCard, taken);
    }

    protected MoveAction beatCard(Card topCard, Card beatCard, List<Card> pot) {
        pot.add(beatCard);
        hand.remove(beatCard);
        return new MoveAction.Beat(topCard, beatCard);
    }

    /**
     * Domain-level description of one atomic action a player took on their turn.
     * The engine maps each of these to a {@link ru.anseranser.event.GameEvent};
     * the player itself stays unaware of the event/output layer.
     */
    public sealed interface MoveAction {
        /** This player led (played the first card of a trick). */
        record Led(Card card) implements MoveAction {}
        /** This player beat the attacking card with another card. */
        record Beat(Card attacking, Card beating) implements MoveAction {}
        /** This player could not beat the top card and took the whole pot. */
        record TookPot(Card topCard, int potSize) implements MoveAction {}
    }
}
