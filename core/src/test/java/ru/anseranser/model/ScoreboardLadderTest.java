package ru.anseranser.model;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.GameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks down rules.md §6: the card pushed to the loser's scoreboard is the
 * <em>next rank required</em> for that suit's ladder — not merely the lowest
 * trump lying in the pile.
 *
 * <p>This is the invariant finding C3 worried about. {@code Game.endRound()}
 * previously selected the minimum-rank trump from the loser's hand plus the pot;
 * the two are equivalent under the game's invariants (every lower trump already
 * sits on the board, and the one required card must be in this pile — the
 * round's last non-empty hand plus the pot), but the rule is stated in terms of
 * the required rank. This test pins the equivalence across whole games, both
 * deck sizes and many seeds, so the implementation can name the required rank
 * directly without changing behavior.
 */
class ScoreboardLadderTest {

    private static final long SEEDS = 300;

    @Test
    void pushedCardIsTheNextRequiredRankForEveryRound() {
        for (DeckSize deckSize : DeckSize.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                assertLadderPerRound(deckSize, seed);
            }
        }
    }

    private static void assertLadderPerRound(DeckSize deckSize, long seed) {
        Game game = new Game(deckSize);
        game.setRng(new SplitMix64(seed));

        List<GameEvent.RoundEnded> roundEnds = new ArrayList<>();
        game.setListener(event -> {
            if (event instanceof GameEvent.RoundEnded e) {
                roundEnds.add(e);
            }
        });

        Game.GameDriver driver = game.createDriver();
        driver.startGame();

        int guard = 0;
        while (!driver.isGameOver() && guard++ < 50_000) {
            while (driver.step()) {
                // run the round move by move
            }

            Map<Card.Suit, List<Card>> before = game.getScoreboard();
            int endsBefore = roundEnds.size();
            driver.finishRound();
            assertEquals(endsBefore + 1, roundEnds.size(),
                    "exactly one RoundEnded per finished round (" + deckSize + ", seed " + seed + ")");

            GameEvent.RoundEnded e = roundEnds.get(roundEnds.size() - 1);
            Card.Rank expected = nextRank(before.get(e.loser()).get(0).rank());

            if (e.pushedToScoreboard() != null) {
                assertNotNull(expected, "a card was pushed although the ladder is complete ("
                        + deckSize + ", seed " + seed + ")");
                assertEquals(expected, e.pushedToScoreboard().rank(),
                        "pushed card must be the next required rank (" + deckSize + ", seed " + seed + ")");
                assertEquals(e.loser(), e.pushedToScoreboard().suit(),
                        "pushed card must belong to the loser's suit (" + deckSize + ", seed " + seed + ")");
                assertEquals(expected, e.scoreboard().get(e.loser()).get(0).rank(),
                        "the pushed card must top the ladder (" + deckSize + ", seed " + seed + ")");
            } else {
                assertNull(expected, "no card pushed although the ladder is not complete ("
                        + deckSize + ", seed " + seed + ")");
            }
        }
    }

    /** The rank that follows {@code rank} on a ladder, or {@code null} at the top. */
    private static Card.Rank nextRank(Card.Rank rank) {
        Card.Rank[] ranks = Card.Rank.values();
        int next = rank.ordinal() + 1;
        return next >= ranks.length ? null : ranks[next];
    }
}
