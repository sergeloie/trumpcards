package ru.anseranser.model.determinism;

import ru.anseranser.model.AiDecisionStrategy;
import ru.anseranser.model.Card;
import ru.anseranser.model.DecisionStrategy;
import ru.anseranser.model.Player;

import java.util.List;

/**
 * A {@link DecisionStrategy} that leads a specific card (when it is in hand) and
 * otherwise behaves like {@link AiDecisionStrategy}. Defense decisions always
 * fall back to the AI heuristic. Used by golden scenarios to make a chosen
 * attacker lead an exact card, so the resulting rule (e.g. "defender cannot beat
 * → takes the pot") is unambiguous.
 */
final class LeadCardStrategy implements DecisionStrategy {

    private final Card lead;
    private final AiDecisionStrategy fallback = new AiDecisionStrategy();

    LeadCardStrategy(Card lead) {
        this.lead = lead;
    }

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        return hand.contains(lead) ? lead : fallback.chooseLeadCard(player, hand);
    }

    @Override
    public Card chooseDefenseCard(Player player, Card attacking, List<Card> validDefenses) {
        return fallback.chooseDefenseCard(player, attacking, validDefenses);
    }
}
