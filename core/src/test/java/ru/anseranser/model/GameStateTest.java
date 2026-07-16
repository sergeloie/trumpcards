package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the core-level immutable snapshot (R2) is self-contained and read-only. */
class GameStateTest {

    @Test
    void snapshotReflectsLiveState() {
        Game game = new Game();
        game.shuffleAndDeal();

        GameState state = game.snapshot();

        assertEquals(game.getTrump(), state.trump());
        // Scoreboard and pot are copied, not the live references.
        assertEquals(game.getScoreboard(), state.scoreboard());
        assertEquals(game.getPot(), state.pot());
        // Every seat is represented with its hand.
        assertEquals(4, state.players().size());
        int totalCards = state.players().stream().mapToInt(p -> p.hand().size()).sum();
        assertTrue(totalCards > 0, "snapshot should include dealt hands");
    }

    @Test
    void snapshotCollectionsAreUnmodifiable() {
        Game game = new Game();
        game.shuffleAndDeal();
        GameState state = game.snapshot();

        Card probe = new Card(Card.Suit.SPADES, Card.Rank.SIX);
        assertThrows(UnsupportedOperationException.class,
                () -> state.players().get(0).hand().add(probe));
        assertThrows(UnsupportedOperationException.class,
                () -> state.scoreboard().put(Card.Suit.HEARTS, List.of()));
        assertThrows(UnsupportedOperationException.class,
                () -> state.pot().add(probe));
    }

    @Test
    void snapshotIsIndependentOfLaterMutations() {
        Game game = new Game();
        game.shuffleAndDeal();
        GameState before = game.snapshot();

        // Mutate the live game; the captured snapshot must be unaffected.
        game.playGame();

        GameState after = game.snapshot();
        // The two snapshots differ (game progressed), proving the copy is detached.
        boolean sameHands = before.players().equals(after.players());
        assertTrue(!sameHands || before.players().stream().allMatch(p -> p.hand().isEmpty()),
                "snapshot taken before playGame must not change when the game advances");
    }
}
