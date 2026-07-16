package ru.anseranser.model;

import java.util.List;

/**
 * Decides a player's moves. The {@link Player} entity holds no knowledge of
 * where a decision comes from (AI heuristic, human input, network, replay); it
 * only delegates to this strategy. This keeps the domain model free of any
 * presentation / input dependency.
 *
 * <p>Implementations: {@link AiDecisionStrategy} (pure, in the model package)
 * and {@code HumanDecisionStrategy} (in the {@code input} package, delegating to
 * an {@code InputProvider}).</p>
 */
public interface DecisionStrategy {

    /** Choose which card to lead with from the given hand. */
    Card chooseLeadCard(Player player, List<Card> hand);

    /** Choose which card to defend {@code attacking} with, among valid defenses. */
    Card chooseDefenseCard(Player player, Card attacking, List<Card> validDefenses);

    /** Whether this strategy is driven by a human (used to emit await-input signals). */
    default boolean isHuman() {
        return false;
    }
}
