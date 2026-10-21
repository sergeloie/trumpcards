package ru.anseranser.engine;

import org.junit.jupiter.api.Test;
import ru.anseranser.model.GameSimulator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The golden gate: the old engine and the new engine play the same game.
 *
 * <p>Both are driven with the same seed, the same setup and the same policy,
 * and their traces are compared entry by entry. The old engine has no steps:
 * its trace is its event stream, so the new engine's events have to carry the
 * same facts.
 */
class GoldenGateTest {

    /** The seeds the gate compares on every run. */
    private static final int SEED_COUNT = 200;

    /** The seeds whose game hits the safety bound on the turns of a round. */
    private static final int[] PATHOLOGICAL_SEEDS = {12129, 24847, 30649, 49128};

    @Test
    void theTwoEnginesPlayTheSameGame() {
        for (int seed = 0; seed < SEED_COUNT; seed++) {
            assertSameTrace(seed);
        }
    }

    @Test
    void thePathologicalSeedsHitTheBoundAtTheSamePoint() {
        for (int seed : PATHOLOGICAL_SEEDS) {
            GameSimulator.Replay replay = new GameSimulator(1, seed, null).replay(seed);
            assertTrue(replay.result().cappedRounds() > 0,
                    "seed " + seed + " must hit the bound in the old engine");
            assertSameTrace(seed);
        }
    }

    private static void assertSameTrace(int seed) {
        List<String> oldTrace = OldEngine.trace(seed);
        List<String> newTrace = NewEngine.trace(seed);
        assertEquals(oldTrace, newTrace,
                "seed " + seed + " diverges at step " + firstDifference(oldTrace, newTrace));
    }

    private static int firstDifference(List<String> oldTrace, List<String> newTrace) {
        int limit = Math.min(oldTrace.size(), newTrace.size());
        for (int i = 0; i < limit; i++) {
            if (!oldTrace.get(i).equals(newTrace.get(i))) {
                return i;
            }
        }
        return limit;
    }
}
