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
     * The decisions a player can face on a turn. This enum is the single source
     * of truth for the "what do I do now" rule (finding C1): it is consulted by
     * the synchronous {@link #makeMove(List)} path and, unchanged, by the
     * stepwise {@link Game.GameDriver} — for both AI and human seats, blocking
     * and resume-based ({@code asyncInput}) alike.
     */
    public enum ActionKind { LEAD, DEFEND, TAKE_POT }

    /**
     * Decide what this player must do at the current decision point. This is the
     * <em>only</em> place the lead/defend/take-pot branching is expressed.
     *
     * <p>Rule: a fresh trick (or an empty pot) is led; otherwise the player
     * defends the top card if any legal defense exists, and takes the whole pot
     * when none does.
     *
     * @param pot       the cards in the middle of the table, top card last
     * @param freshLead {@code true} when a new trick must be led even if the pot
     *                  is not empty (a player who just beat the top card leads
     *                  the next trick)
     */
    public ActionKind nextActionKind(List<Card> pot, boolean freshLead) {
        if (freshLead || pot.isEmpty()) {
            return ActionKind.LEAD;
        }
        Card top = pot.get(pot.size() - 1);
        return validDefenses(top).isEmpty() ? ActionKind.TAKE_POT : ActionKind.DEFEND;
    }

    /**
     * Play this player's turn. The method is pure with respect to the output
     * layer: it mutates only this player's hand and the shared {@code pot}, and
     * returns the ordered list of domain actions that occurred (lead / beat /
     * take-pot). The engine translates those actions into {@link
     * ru.anseranser.event.GameEvent}s — the player never references the event
     * system, keeping the domain model free of presentation concerns.
     *
     * <p>Built on {@link #nextActionKind(List, boolean)} plus the {@code apply*}
     * appliers below, so the same rule steers the driver's resume-based path.
     */
    public List<MoveAction> makeMove(List<Card> pot) {
        List<MoveAction> actions = new ArrayList<>();
        boolean freshLead = false;
        while (!hand.isEmpty()) {
            ActionKind kind = nextActionKind(pot, freshLead);
            if (kind == ActionKind.LEAD) {
                actions.add(applyLead(pot, null));
                break; // a lead ends the turn
            }
            if (kind == ActionKind.TAKE_POT) {
                actions.add(applyTakePot(pot));
                break; // taking the pot ends the turn
            }
            Card top = pot.get(pot.size() - 1);
            Card chosen = strategy.chooseDefenseCard(this, top, validDefenses(top));
            actions.add(applyDefense(pot, chosen));
            freshLead = true; // after beating, the same player leads a new trick
        }
        return actions;
    }

    /**
     * Apply a {@link ActionKind#LEAD} decision: lead a card into the pot. When
     * {@code forced} is non-null it is used as the led card (resume/async input
     * path, where the human's choice was supplied out-of-band); otherwise the
     * {@link DecisionStrategy} decides. The forced card must currently be in
     * this player's hand.
     */
    public MoveAction applyLead(List<Card> pot, Card forced) {
        Card leadCard = forced != null ? forced : chooseLeadCard();
        if (forced != null && !hand.contains(forced)) {
            throw new IllegalArgumentException("forced lead card not in hand: " + forced);
        }
        pot.add(leadCard);
        hand.remove(leadCard);
        return new MoveAction.Led(leadCard);
    }

    /** Apply a {@link ActionKind#TAKE_POT} decision: take the whole pot into hand. */
    public MoveAction applyTakePot(List<Card> pot) {
        Card top = pot.get(pot.size() - 1);
        int taken = pot.size();
        collectPot(pot);
        return new MoveAction.TookPot(top, taken);
    }

    /** Apply a {@link ActionKind#DEFEND} decision: beat the current top card. */
    public MoveAction applyDefense(List<Card> pot, Card defense) {
        Card top = pot.get(pot.size() - 1);
        pot.add(defense);
        hand.remove(defense);
        return new MoveAction.Beat(top, defense);
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
