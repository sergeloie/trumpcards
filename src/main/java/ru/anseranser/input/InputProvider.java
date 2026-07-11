package ru.anseranser.input;

import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;

/**
 * Abstraction over "where does the human's choice come from".
 *
 * This is the seam introduced in refactor Stage 1: it removes the hard
 * dependency on {@code Scanner}/console from the model. A console game, a
 * mobile app, and a desktop app each provide their own implementation
 * (buttons, touch, text input, network, etc.).
 */
public interface InputProvider {

    /**
     * Ask the human player to pick a lead card from their whole hand.
     *
     * @param player the human player making the choice
     * @param hand   the current cards in hand (unmodified; caller removes the chosen one)
     * @return the chosen card (must be one of {@code hand})
     */
    Card chooseLeadCard(Player player, List<Card> hand);

    /**
     * Ask the human player to pick a card that beats {@code attacking},
     * among the provided valid defenses.
     *
     * @param player          the human player defending
     * @param attacking       the card that must be beaten
     * @param validDefenses   cards from hand that legally beat {@code attacking}, ordered
     * @return the chosen defending card (must be one of {@code validDefenses})
     */
    Card chooseDefense(Player player, Card attacking, List<Card> validDefenses);
}
