package ru.anseranser.model;

import java.util.Comparator;
import java.util.List;

/**
 * Default AI decision strategy: leads with the weakest card and defends with
 * the weakest legal card. Pure domain logic — no I/O, no input dependency.
 */
public class AiDecisionStrategy implements DecisionStrategy {

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        return hand.stream()
                .min(Comparator
                        .comparing((Card c) -> c.suit() == player.getTrump()) // non-trumps first
                        .thenComparing(c -> c.rank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to turn"));
    }

    @Override
    public Card chooseDefenseCard(Player player, Card attacking, List<Card> validDefenses) {
        // validDefenses is already sorted weakest-first by the caller.
        return validDefenses.get(0);
    }
}
