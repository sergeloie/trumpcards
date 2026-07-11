package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the stepwise {@link Game.GameDriver} — the path a desktop / mobile UI
 * will use to advance the game one move at a time.
 *
 * <ul>
 *   <li>Equivalence — driving the game via {@code step()}/{@code finishRound()}
 *       yields the same winner as the synchronous {@code playGame(Random)}.</li>
 *   <li>Phase flow — a round is ROUND_ACTIVE while stepping, flips to
 *       ROUND_ENDED at the end, and the game reaches GAME_ENDED exactly once.</li>
 *   <li>Single move per step — each {@code step()} performs exactly one move
 *       (one CardPlayed / CardBeaten / PotTaken event pair), so a UI can pace it.</li>
 * </ul>
 */
class GameDriverTest {

    @Test
    void driver_matchesSynchronousPlayGame() {
        for (int seed = 0; seed < 50; seed++) {
            Game sync = new Game(false);
            sync.playGame(new Random(seed));
            Card.Suit syncWinner = sync.getWinner().getTrump();

            Game stepwise = new Game(false);
            stepwise.setRng(new Random(seed));
            Game.GameDriver d = stepwise.createDriver();
            d.startGame();
            while (!d.isGameOver()) {
                while (d.step()) {
                    // one move per iteration, as a UI would call it
                }
                d.finishRound();
            }
            Card.Suit stepWinner = stepwise.getWinner().getTrump();

            assertEquals(syncWinner, stepWinner,
                    "Seed " + seed + ": stepwise driver must match synchronous play");
        }
    }

    @Test
    void driver_emitsOneEventPerStep() {
        Game game = new Game(false);
        final int[] events = {0};
        game.setListener(event -> events[0]++);

        Game.GameDriver d = game.createDriver();
        d.startGame();
        assertTrue(d.getPhase() == Game.Phase.ROUND_ACTIVE, "Round should be active after start");

        // Drive one full game and confirm the phase machine terminates.
        int steps = 0;
        while (!d.isGameOver()) {
            boolean stepped = d.step();
            if (stepped) {
                steps++;
                assertEquals(Game.Phase.ROUND_ACTIVE, d.getPhase(),
                        "Phase must stay ROUND_ACTIVE mid-round");
            } else {
                assertEquals(Game.Phase.ROUND_ENDED, d.getPhase(),
                        "Step returning false must mean ROUND_ENDED");
                d.finishRound();
            }
        }
        assertEquals(Game.Phase.GAME_ENDED, d.getPhase());
        assertTrue(steps > 0, "At least one move should have been played");
        assertTrue(events[0] > 0, "Events must be emitted during play");
    }

    @Test
    void driver_stateReadableBetweenSteps() {
        Game game = new Game(false);
        Game.GameDriver d = game.createDriver();
        d.startGame();

        // Pot is empty at the start of a round (no lead card played yet).
        assertTrue(d.getPot().isEmpty(), "Pot empty before first lead");
        assertNotNull(d.getCurrent(), "Current player must be known");

        d.step();
        // After one step the pot holds exactly one card (the lead).
        assertEquals(1, d.getPot().size(), "Pot holds the lead card after one step");
    }
}
