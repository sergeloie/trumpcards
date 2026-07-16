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
