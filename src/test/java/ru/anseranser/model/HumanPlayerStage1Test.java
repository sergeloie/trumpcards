package ru.anseranser.model;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.NopListener;
import ru.anseranser.input.InputProvider;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for the human-player path without a real console: a scripted
 * {@link InputProvider} answers every choice with the first available option.
 * Verifies that {@code HumanPlayer} works through the {@code InputProvider}
 * seam introduced in refactor Stage 1 (no Scanner in the model).
 */
class HumanPlayerStage1Test {

    @Test
    void humanGameCompletes_withScriptedInputProvider() {
        Game game = new Game(true);
        game.setListener(NopListener.INSTANCE);

        Queue<Integer> choices = new ArrayDeque<>();
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

        Player start = game.getPlayers().getRandom();
        Player current = start;
        do {
            if (current instanceof HumanPlayer human) {
                human.setInput(scripted);
            }
            current = game.getPlayers().getNext(current);
        } while (current != start);

        assertDoesNotThrow(game::playGame);

        Player winner = game.getWinner();
        assertNotNull(winner, "A winner should emerge from a human-inclusive game");
        assertEquals(1, countGamers(game), "Exactly one player should remain a gamer");
    }

    private int countGamers(Game game) {
        int gamers = 0;
        Player start = game.getPlayers().getRandom();
        Player current = start;
        do {
            if (current.isGamer()) gamers++;
            current = game.getPlayers().getNext(current);
        } while (current != start);
        return gamers;
    }
}
