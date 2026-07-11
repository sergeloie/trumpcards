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
    private boolean rounder = true;
    @Setter
    private TurnOrder order;
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

    protected void takePot(List<Card> pot) {
        hand.addAll(pot);
        pot.clear();
    }


    @Override
    public String toString() {
        return "Player(trump=" + trump + ", cards=" + hand.size() + ")";
    }

    public Card chooseLeadCard() {
        Player current = this;
        for (int i = 0; i < order.size() - 1; i++) {
            current = order.previous(current);
            Card.Suit suit = current.getTrump();

            Optional<Card> smallest = hand.stream()
                    .filter(c -> c.suit() == suit)
                    .min(Comparator.comparing(c -> c.rank().getValue()));

            if (smallest.isPresent()) {
                return smallest.get();
            }
        }

        // First try to play lowest trump
        Optional<Card> lowestTrump = hand.stream()
                .filter(c -> c.suit() == trump)
                .min(Comparator.comparing(c -> c.rank().getValue()));

        if (lowestTrump.isPresent()) {
            return lowestTrump.get();
        }

        // If no trumps, play lowest non-trump
        return hand.stream()
                .min(Comparator.comparing(c -> c.rank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to turn"));
    }

    public void makeMove(List<Card> bank) {
        if (!rounder) return;

        if (bank.isEmpty()) {
            playLeadCard(bank);
            if (hand.isEmpty()) rounder = false;
            return;
        }

        Card topCard = bank.getLast();
        Optional<Card> defense = weakestDefense(topCard);

        if (defense.isEmpty()) {
            takeBank(topCard, bank);
            return;
        }

        beatCard(topCard, defense.get(), bank);

        if (hand.isEmpty()) {
            rounder = false;
            return;
        }

        playLeadCard(bank);
        if (hand.isEmpty()) rounder = false;
    }

    protected void playLeadCard(List<Card> bank) {
        Card leadCard = chooseLeadCard();
        bank.add(leadCard);
        hand.remove(leadCard);
        listener.onEvent(new GameEvent.CardPlayed(this, leadCard));
    }

    protected void takeBank(Card topCard, List<Card> bank) {
        listener.onEvent(new GameEvent.PotTaken(this, topCard, bank.size()));
        takePot(bank);
    }

    protected void beatCard(Card topCard, Card beatCard, List<Card> bank) {
        bank.add(beatCard);
        hand.remove(beatCard);
        listener.onEvent(new GameEvent.CardBeaten(this, topCard, beatCard));
    }
}
