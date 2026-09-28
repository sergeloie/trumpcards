package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import ru.anseranser.input.HumanDecisionStrategy;
import ru.anseranser.input.InputProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the resume-based, non-blocking input path of {@link Game.GameDriver} (R4).
 * The driver is put in async mode; instead of blocking on an {@link InputProvider},
 * it emits {@link ru.anseranser.event.GameEvent.AwaitingHumanInput} and yields, and the
 * test supplies the human's card via {@link Game.GameDriver#resume(Card)}.
 */
class GameDriverAsyncTest {

    /** A no-op provider: in async mode the strategy is never asked (we resume instead). */
    private static final InputProvider DUMMY = new InputProvider() {
        @Override public Card chooseLeadCard(Player p, List<Card> hand) { throw new AssertionError("should not be called"); }
        @Override public Card chooseDefense(Player p, Card attacking, List<Card> valid) { throw new AssertionError("should not be called"); }
    };

    @Test
    void asyncGameCompletesWithHumanResumes() {
        List<Player> players = List.of(
                new Player(Card.Suit.SPADES, new HumanDecisionStrategy(DUMMY)),
                new Player(Card.Suit.CLUBS),
                new Player(Card.Suit.DIAMONDS),
                new Player(Card.Suit.HEARTS));
        Game game = new Game(players);
        Game.GameDriver d = game.createDriver();
        d.setAsyncInput(true);
        d.startGame();

        int safety = 0;
        while (!d.isGameOver()) {
            while (d.step()) {
                if (d.isAwaitingHumanInput()) {
                    // Supply the human's choice. The driver tells us exactly which
                    // cards are legal; a UI would enable only those. If the turn
                    // needs two decisions (e.g. lead after a beat) it yields again.
                    while (d.isAwaitingHumanInput()) {
                        Game.GameDriver.InputRequest req = d.pendingInput();
                        d.resume(req.validCards().get(0));
                    }
                }
                if (++safety > 1_000_000) throw new AssertionError("game did not terminate");
            }
            d.finishRound();
        }

        // Exactly one winner remains.
        long gamers = game.snapshot().players().stream().filter(GameState.PlayerState::gamer).count();
        assertEquals(1, gamers);
        // Card conservation: all 36 cards still accounted for.
        assertEquals(36, game.allCards().size());
    }

    @Test
    void asyncSaveAndRestore_resumesThePausedHumanTurn() {
        Game game = humanSpadesGame(42L);
        Game.GameDriver d = game.createDriver();
        d.setAsyncInput(true);
        d.startGame();

        int guard = 0;
        while (!d.isAwaitingHumanInput() && !d.isGameOver()) {
            if (!d.step()) {
                d.finishRound();
            }
            if (++guard > 1_000_000) throw new AssertionError("no human turn reached");
        }
        assertTrue(d.isAwaitingHumanInput(), "the human seat must be asked to move");
        Game.GameDriver.InputRequest before = d.pendingInput();

        // Save while paused, round-trip through JSON, restore on a fresh engine.
        SavedGame reloaded = SavedGame.fromJson(d.save().toJson());
        Game.GameDriver resumed = Game.restore(reloaded, suit ->
                suit == Card.Suit.SPADES
                        ? new HumanDecisionStrategy(DUMMY)
                        : new AiDecisionStrategy());

        // The pause (awaiting + pending action + offered cards) must survive the round-trip.
        assertTrue(resumed.isAwaitingHumanInput(), "restore must revive the paused human turn");
        assertEquals(before.kind(), resumed.pendingInput().kind());
        assertEquals(before.validCards(), resumed.pendingInput().validCards());

        // Resume both with the same card and drive them to the end: they must match.
        d.resume(before.validCards().get(0));
        resumed.resume(before.validCards().get(0));
        runToCompletion(d);
        runToCompletion(resumed);

        assertEquals(d.getGame().getWinner().getTrump(), resumed.getGame().getWinner().getTrump(),
                "a game resumed from a paused save must finish like the original");
        assertEquals(36, resumed.getGame().allCards().size(), "card conservation broken after resume");
    }

    private static Game humanSpadesGame(long seed) {
        List<Player> players = List.of(
                new Player(Card.Suit.SPADES, new HumanDecisionStrategy(DUMMY)),
                new Player(Card.Suit.CLUBS),
                new Player(Card.Suit.DIAMONDS),
                new Player(Card.Suit.HEARTS));
        Game game = new Game(players);
        game.setRng(new SplitMix64(seed));
        return game;
    }

    private static void runToCompletion(Game.GameDriver d) {
        while (!d.isGameOver()) {
            while (d.isAwaitingHumanInput()) {
                d.resume(d.pendingInput().validCards().get(0));
            }
            if (!d.step()) {
                d.finishRound();
            }
        }
    }

    @Test
    void resumeWithoutAwaitThrows() {
        Game game = new Game();
        Game.GameDriver d = game.createDriver();
        d.setAsyncInput(true);
        d.startGame();
        // Not awaiting input yet (current may be an AI) — resume must be rejected.
        boolean threw = false;
        try {
            d.resume(new Card(Card.Suit.SPADES, Card.Rank.SIX));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw);
    }
}
