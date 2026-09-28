package ru.anseranser.model.determinism;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.GameEvent;
import ru.anseranser.model.Game;
import ru.anseranser.model.GameSimulator;
import ru.anseranser.model.SplitMix64;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Seed-mode reproducibility: the same RNG seed must drive the
 * {@link Game.GameDriver} through an identical sequence of events. Two runs are
 * canonicalized with {@link EventLog} and compared element by element.
 *
 * <p>Includes a genuinely pathological seed: {@code 12129} caps exactly one round
 * at {@code MAX_ROUND_MOVES}. The requirement is that it still replays
 * identically — a capped round must be reproducible, not skipped.</p>
 */
class ReplayDeterminismTest {

    private static List<String> record(long seed) {
        Game game = new Game();
        game.setRng(new SplitMix64(seed));
        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);

        Game.GameDriver d = game.createDriver();
        d.startGame();
        while (!d.isGameOver()) {
            while (d.step()) {
                // one move per step
            }
            d.finishRound();
        }
        return EventLog.describe(events);
    }

    @Test
    void eventStream_isIdenticalAcrossTwoRuns() {
        for (long seed : new long[]{0, 1, 7, 42, 100}) {
            assertEquals(record(seed), record(seed), "Seed " + seed + " must replay identically");
        }
    }

    @Test
    void pathologicalCappedSeed_isReproducible_andActuallyCapped() {
        // Confirm 12129 really is pathological (caps a round), then prove it replays.
        GameSimulator.Replay first = new GameSimulator(1, 12129, null).replay(12129);
        assertTrue(first.result().cappedRounds() > 0,
                "Seed 12129 is expected to hit MAX_ROUND_MOVES at least once");

        List<String> runA = record(12129);
        List<String> runB = record(12129);
        assertEquals(runA, runB, "A capped round must still replay bit-for-bit");

        assertTrue(runA.stream().anyMatch(s -> s.startsWith("RoundEnded")),
                "The replay must contain round-end events");
        assertTrue(runA.stream().anyMatch(s -> s.startsWith("TookPot")),
                "The replay must contain move events");
    }
}
