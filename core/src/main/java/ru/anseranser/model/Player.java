package ru.anseranser.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
import ru.anseranser.event.NopListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class Player {

    private final Card.Suit trump;
    private final List<Card> hand = new ArrayList<>();
    @Setter
    private boolean gamer = true;
    @Setter
    protected GameListener listener = NopListener.INSTANCE;

    protected boolean canBeat(Card attacking, Card defending) {
        if (attacking.suit() == defending.suit()) {
            return attacking.rank().getValue() < defending.rank().getValue();
        } else return defending.suit() == trump;
    }

    protected Optional<Card> weakestDefense(Card attacking) {
        return hand.stream()
                .filter(c -> canBeat(attacking, c))
                .min(Comparator
                        .comparing((Card c) -> c.suit() == trump) // non-trumps first (false < true)
                        .thenComparing(c -> c.rank().getValue()));
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
        // Lead with the weakest card by the same seniority scale used for beating:
        // non-trumps rank below our own trump, and within a group by rank value.
        return hand.stream()
                .min(Comparator
                        .comparing((Card c) -> c.suit() == trump) // non-trumps first (false < true)
                        .thenComparing(c -> c.rank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to turn"));
    }

    public void makeMove(List<Card> pot) {
        if (hand.isEmpty()) return;

        if (pot.isEmpty()) {
            playLeadCard(pot);
            return;
        }

        Card topCard = pot.getLast();
        Optional<Card> defense = weakestDefense(topCard);

        if (defense.isEmpty()) {
            takePot(topCard, pot);
            return;
        }

        Card chosen = chooseDefenseCard(topCard, defense.get());
        beatCard(topCard, chosen, pot);

        if (hand.isEmpty()) {
            return;
        }

        playLeadCard(pot);
    }

    /**
     * Decision hook: which card to defend {@code attacking} with.
     * <p>
     * The base (AI) implementation always plays the weakest legal defense.
     * {@code HumanPlayer} overrides this to let the user pick. The engine
     * mechanics in {@link #makeMove} (pot/hand mutation, events) are shared and
     * never duplicated — subclasses only override the decision, not the plumbing.
     *
     * @param attacking the card that must be beaten
     * @param weakest   the weakest legal defense (already computed, never null here)
     * @return the card to defend with (must legally beat {@code attacking})
     */
    protected Card chooseDefenseCard(Card attacking, Card weakest) {
        return weakest;
    }

    protected void playLeadCard(List<Card> pot) {
        Card leadCard = chooseLeadCard();
        pot.add(leadCard);
        hand.remove(leadCard);
        listener.onEvent(new GameEvent.CardPlayed(this, leadCard));
    }

    protected void takePot(Card topCard, List<Card> pot) {
        int taken = pot.size();
        collectPot(pot);
        // Fire AFTER the pot is cleared so listeners observe a consistent state:
        // the pot is gone and the table is empty (the next move is a lead, not a beat).
        listener.onEvent(new GameEvent.PotTaken(this, topCard, taken));
    }

    protected void beatCard(Card topCard, Card beatCard, List<Card> pot) {
        pot.add(beatCard);
        hand.remove(beatCard);
        listener.onEvent(new GameEvent.CardBeaten(this, topCard, beatCard));
    }
}
