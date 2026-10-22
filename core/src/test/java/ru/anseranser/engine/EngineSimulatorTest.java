package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The invariants of the new engine, checked over many games: card
 * conservation, exactly one winner, termination and reproducibility.
 *
 * <p>The model is the old {@code GameSimulatorTest}: the same seed range, the
 * same four invariants, and the same honesty about the rounds that hit the
 * safety bound.
 */
class EngineSimulatorTest {

    /** The suits of the four-player table, in the canonical order. */
    private static final List<Card.Suit> FOUR_SUITS = List.of(SPADES, CLUBS, DIAMONDS, HEARTS);

    /** The suits of the three-player table. */
    private static final List<Card.Suit> THREE_SUITS = List.of(SPADES, CLUBS, DIAMONDS);

    /** The suits of the two-player table. */
    private static final List<Card.Suit> TWO_SUITS = List.of(SPADES, CLUBS);

    /** The seed range the old suite scanned. */
    private static final int SEED_COUNT = 50_000;

    /** The seeds whose game hits the safety bound on the turns of a round. */
    private static final List<Long> PATHOLOGICAL_SEEDS = List.of(12129L, 24847L, 30649L, 49128L);

    @Test
    void theInvariantsHoldAcrossEverySeedOfTheRange() {
        List<EngineSimulator.Result> results =
                EngineSimulator.run(RankSet.SIX_TO_ACE, FOUR_SUITS, 0, SEED_COUNT);

        assertEquals(SEED_COUNT, results.size());

        List<Long> capped = new ArrayList<>();
        for (EngineSimulator.Result result : results) {
            String seed = "Seed " + result.seed() + ": ";
            assertEquals(result.expectedCards(), result.totalCards(),
                    seed + "every card of the deck must be in play at the end of the game");
            assertEquals(result.expectedCards(), result.distinctCards(),
                    seed + "every card of the deck must be in play once, none lost and none duplicated");
            assertEquals(1, result.activeSeats(),
                    seed + "exactly one seat must be left in the game");
            assertNotNull(result.winnerTrump(),
                    seed + "a winner must exist");
            assertTrue(result.rounds() >= 1,
                    seed + "the game must play at least one round");
            assertTrue(result.invariantsHold(),
                    seed + "the invariants must hold");
            if (result.cappedRounds() > 0) {
                capped.add(result.seed());
            }
        }

        assertEquals(PATHOLOGICAL_SEEDS, capped,
                "the rounds that hit the safety bound must be reported, and only the pathological deals may hit it");
    }

    @Test
    void theSameSeedGivesTheSameGame() {
        for (long seed = 0; seed < 200; seed++) {
            Setup setup = new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, seed);
            assertEquals(EngineSimulator.finalState(setup), EngineSimulator.finalState(setup),
                    "Seed " + seed + ": the same seed must give the same game");
        }
    }

    @Test
    void everySuitWinsSomewhereInTheRange() {
        List<EngineSimulator.Result> results =
                EngineSimulator.run(RankSet.SIX_TO_ACE, FOUR_SUITS, 100, 500);
        Map<Card.Suit, Long> bySuit = results.stream()
                .collect(Collectors.groupingBy(EngineSimulator.Result::winnerTrump, Collectors.counting()));

        assertEquals(4, bySuit.size(),
                "across 500 seeds every suit should win at least once (got: " + bySuit + ")");
    }

    @Test
    void thePathologicalSeedsHitTheBoundAndAreReported() {
        for (long seed : PATHOLOGICAL_SEEDS) {
            EngineSimulator.Result result =
                    EngineSimulator.result(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, seed));

            assertTrue(result.cappedRounds() > 0,
                    "seed " + seed + " must be reported as capped, not hidden");
            assertTrue(result.invariantsHold(),
                    "seed " + seed + " must keep the invariants even when the bound fires");
        }
    }

    @Test
    void theSmallTablesKeepTheInvariants() {
        for (List<Card.Suit> trumps : List.of(TWO_SUITS, THREE_SUITS)) {
            int expectedCards = RankSet.SIX_TO_ACE.cardCount(trumps.size());
            List<EngineSimulator.Result> results =
                    EngineSimulator.run(RankSet.SIX_TO_ACE, trumps, 0, 5_000);

            for (EngineSimulator.Result result : results) {
                String seed = "Seed " + result.seed() + " of " + trumps.size() + " seats: ";
                assertEquals(expectedCards, result.totalCards(),
                        seed + "the deck must hold a card for every rank of every suit in play");
                assertEquals(expectedCards, result.distinctCards(),
                        seed + "every card of the deck must be in play once");
                assertEquals(1, result.activeSeats(),
                        seed + "exactly one seat must be left in the game");
                assertNotNull(result.winnerTrump(),
                        seed + "a winner must exist");
                assertTrue(result.rounds() >= 1,
                        seed + "the game must play at least one round");
            }
        }
    }

    @Test
    void theSmallTablesAreReproducible() {
        for (long seed = 0; seed < 100; seed++) {
            for (List<Card.Suit> trumps : List.of(TWO_SUITS, THREE_SUITS)) {
                Setup setup = new Setup(RankSet.SIX_TO_ACE, trumps, seed);
                assertEquals(EngineSimulator.finalState(setup), EngineSimulator.finalState(setup),
                        "Seed " + seed + " of " + trumps.size() + " seats: the same seed must give the same game");
            }
        }
    }
}
