package ru.anseranser.input;

import ru.anseranser.model.Card;
import ru.anseranser.model.DecisionStrategy;
import ru.anseranser.model.Player;

import java.util.List;

/**
 * Human-driven decision strategy. Delegates every choice to an
 * {@link InputProvider}. This is the only place where the input layer is
 * referenced from a decision — the domain {@link Player} stays input-free.
 */
public class HumanDecisionStrategy implements DecisionStrategy {

    private final InputProvider input;

    public HumanDecisionStrategy(InputProvider input) {
        this.input = input;
    }

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        return input.chooseLeadCard(player, hand);
    }

    @Override
    public Card chooseDefenseCard(Player player, Card attacking, List<Card> validDefenses) {
        return input.chooseDefense(player, attacking, validDefenses);
    }

    @Override
    public boolean isHuman() {
        return true;
    }
}
