package ru.anseranser.input;

import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;

/**
 * No-op input provider used in headless contexts (tests, AI-only games) where
 * no human choice is ever requested. Any call is a programming error in that
 * context, hence it throws.
 */
public enum NoopInputProvider implements InputProvider {
    INSTANCE;

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        throw new UnsupportedOperationException("No human input provider configured");
    }

    @Override
    public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
        throw new UnsupportedOperationException("No human input provider configured");
    }
}
