package ru.anseranser.model.determinism;

import org.junit.jupiter.api.Test;
import ru.anseranser.model.GameSimulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-level determinism: {@link GameSimulator#replay(int)} captures the whole
 * path of a game (loser, pushed card, elimination flag, move count per round).
 * Two independent runs of the same seed must produce identical paths — not just
 * the same final winner.
 */
class RoundLevelSimulatorTest {

    private static final GameSimulator SIM = new GameSimulator(0, 0, null);

    @Test
    void sameSeed_producesIdenticalRoundPaths() {
        for (int seed = 0; seed < 500; seed++) {
            GameSimulator.Replay a = SIM.replay(seed);
            GameSimulator.Replay b = SIM.replay(seed);
            assertEquals(a.rounds(), b.rounds(), "Seed " + seed + ": round paths must match");
            assertEquals(a.result().winnerTrump(), b.result().winnerTrump(),
                    "Seed " + seed + ": winners must match");
        }
    }

    @Test
    void replayPath_isConsistentWithFinalResult() {
        for (int seed = 0; seed < 200; seed++) {
            GameSimulator.Replay r = SIM.replay(seed);
            assertEquals(r.result().rounds(), r.rounds().size(),
                    "Seed " + seed + ": one snapshot per played round");
            for (GameSimulator.RoundSnapshot round : r.rounds()) {
                assertNotNull(round.loser(), "Seed " + seed + ": every round has a loser");
                assertTrue(round.moves() >= 0);
            }
        }
    }

    @Test
    void replay_capturesCappedRoundPath() {
        GameSimulator.Replay r = new GameSimulator(1, 12129, null).replay(12129);
        assertTrue(r.result().cappedRounds() > 0, "Seed 12129 must cap a round");
        assertTrue(r.rounds().stream().anyMatch(s -> s.moves() >= 10_000),
                "The capped round must report at least MAX_ROUND_MOVES moves");
    }
}
