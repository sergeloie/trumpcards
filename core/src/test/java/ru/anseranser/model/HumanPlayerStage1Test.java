package ru.anseranser.model;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.NopListener;
import ru.anseranser.input.HumanDecisionStrategy;
import ru.anseranser.input.InputProvider;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for the human-player path without a real console: a scripted
 * {@link InputProvider} answers every choice with the first available option.
 * Verifies that a {@link Player} wired with a {@link HumanDecisionStrategy}
 * works through the {@code InputProvider} seam (no Scanner in the model, no
 * {@code HumanPlayer} subclass — the decision strategy is injected).
 */
class HumanPlayerStage1Test {

    @Test
    void humanGameCompletes_withScriptedInputProvider() {
        InputProvider scripted = new InputProvider() {
            @Override
            public Card chooseLeadCard(Player player, List<Card> hand) {
                return hand.get(0);
            }

            @Override
            public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
                return validDefenses.get(0);
            }
        };

        // Composition root: SPADES seat is human (scripted), the rest are AI.
        List<Player> players = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            DecisionStrategy strategy = (suit == Card.Suit.SPADES)
                    ? new HumanDecisionStrategy(scripted)
                    : new AiDecisionStrategy();
            players.add(new Player(suit, strategy));
        }

        Game game = new Game(players);
        game.setListener(NopListener.INSTANCE);

        assertDoesNotThrow(() -> game.playGame());

        Player winner = game.getWinner();
        assertNotNull(winner, "A winner should emerge from a human-inclusive game");
        assertEquals(1, countGamers(game), "Exactly one player should remain a gamer");
    }

    private int countGamers(Game game) {
        int gamers = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) gamers++;
        }
        return gamers;
    }
}
