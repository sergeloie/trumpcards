package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification suite for refactor Stage 7: run many seeded games and assert the
 * engine invariants that prove the refactor (Stages 1–5) preserved behaviour.
 *
 * <ul>
 *   <li>Card conservation — 36 distinct cards across all hands at game end.</li>
 *   <li>Exactly one winner (one active gamer).</li>
 *   <li>Termination — every game completes within a bounded number of rounds.</li>
 *   <li>Determinism — replaying the same seed yields the same winner.</li>
 * </ul>
 */
class GameSimulatorTest {

    @Test
    void invariants_holdAcrossManySeeds() {
        GameSimulator sim = new GameSimulator(200, 1, false);
        List<GameSimulator.Result> results = sim.run();

        assertEquals(200, results.size());

        for (GameSimulator.Result r : results) {
            assertEquals(36, r.totalCards(),
                    "Seed " + r.seed() + ": all 36 cards must be in players' hands at game end");
            assertEquals(36, r.distinctCards(),
                    "Seed " + r.seed() + ": all 36 cards must be distinct (no duplicates/loss)");
            assertEquals(1, r.activeGamers(),
                    "Seed " + r.seed() + ": exactly one player must remain a gamer");
            assertNotNull(r.winnerTrump(),
                    "Seed " + r.seed() + ": a winner must exist");
            assertTrue(r.invariantsHold(),
                    "Seed " + r.seed() + ": composite invariants must hold");
        }
    }

    @Test
    void determinism_sameSeed_sameWinner() {
        for (int seed = 0; seed < 50; seed++) {
            Game g1 = new Game(false);
            Game g2 = new Game(false);
            g1.playGame(new Random(seed));
            g2.playGame(new Random(seed));
            assertEquals(g1.getWinner().getTrump(), g2.getWinner().getTrump(),
                    "Seed " + seed + ": identical seeds must yield the same winner");
        }
    }

    @Test
    void winners_areDistributedAcrossSuits() {
        // Not a hard invariant, but a sanity check that the RNG-driven dealing
        // doesn't collapse every game to one suit (which would hint at a bug).
        GameSimulator sim = new GameSimulator(500, 100, false);
        Map<Card.Suit, Long> bySuit = sim.run().stream()
                .collect(Collectors.groupingBy(
                        GameSimulator.Result::winnerTrump, Collectors.counting()));

        assertEquals(4, bySuit.size(),
                "Across 500 seeds every suit should win at least once (got: " + bySuit + ")");
    }
}
