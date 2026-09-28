package ru.anseranser.model.determinism;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.GameEvent;
import ru.anseranser.model.AiDecisionStrategy;
import ru.anseranser.model.Card;
import ru.anseranser.model.DecisionStrategy;
import ru.anseranser.model.DeckSize;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;
import ru.anseranser.model.ScriptedCardDealer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the two {@link Game.GameDriver} input paths evaluate a game identically
 * (finding C1 / I9). Before the C1 refactor the async path re-implemented the
 * lead/defend/take-pot branching inside {@code humanTurn()}, so a rule change in
 * {@link Player#makeMove(List)} could silently fail to reach the resume-based
 * path. Both now go through {@link Player#nextActionKind(List, boolean)}; this
 * test pins that equivalence down on a fully scripted layout.
 *
 * <p>Only {@link GameEvent.AwaitingHumanInput} is excluded: it is a UI pause
 * signal and legitimately fires once per decision in the async path but once per
 * turn in the blocking path. Every move/round/game event must match exactly.</p>
 */
class SyncAsyncEquivalenceTest {

    /**
     * Deterministic "human": always plays the first offered card. For a {@code LEAD}
     * the offered list is the whole hand in hand order; for a {@code DEFEND} it is
     * {@code validDefenses} — exactly what {@code pendingInput()} hands the UI, so
     * resuming with {@code validCards().get(0)} reproduces this strategy.
     */
    private static final class FirstOfferedCardStrategy implements DecisionStrategy {
        @Override
        public Card chooseLeadCard(Player player, List<Card> hand) {
            return hand.get(0);
        }

        @Override
        public Card chooseDefenseCard(Player player, Card attacking, List<Card> validDefenses) {
            return validDefenses.get(0);
        }

        @Override
        public boolean isHuman() {
            return true;
        }
    }

    private static Game newGame() {
        List<Player> players = new ArrayList<>();
        players.add(new Player(Card.Suit.SPADES, new FirstOfferedCardStrategy())); // human seat
        players.add(new Player(Card.Suit.CLUBS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.DIAMONDS, new AiDecisionStrategy()));
        players.add(new Player(Card.Suit.HEARTS, new AiDecisionStrategy()));

        Game game = new Game(players, DeckSize.THIRTY_SIX);
        game.setCardDealer(ScriptedCardDealer.full(DeckSize.THIRTY_SIX));
        game.setRng(new FixedRng(0)); // deterministic first dealer
        return game;
    }

    /** Blocking path: the human seat is driven through {@link Player#makeMove(List)}. */
    private static List<GameEvent> runSync(Game game) {
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
        return events;
    }

    /** Resume-based path: the human seat is driven through {@code pendingInput()}/{@code resume()}. */
    private static List<GameEvent> runAsync(Game game) {
        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        Game.GameDriver d = game.createDriver();
        d.setAsyncInput(true);
        d.startGame();

        int safety = 0;
        while (!d.isGameOver()) {
            while (d.step()) {
                while (d.isAwaitingHumanInput()) {
                    Game.GameDriver.InputRequest req = d.pendingInput();
                    d.resume(req.validCards().get(0));
                }
                if (++safety > 1_000_000) {
                    throw new AssertionError("async game did not terminate");
                }
            }
            d.finishRound();
        }
        return events;
    }

    private static List<GameEvent> gameEvents(List<GameEvent> events) {
        return events.stream()
                .filter(e -> !(e instanceof GameEvent.AwaitingHumanInput))
                .toList();
    }

    @Test
    void syncAndAsyncPaths_produceIdenticalGameplay() {
        List<GameEvent> sync = gameEvents(runSync(newGame()));
        List<GameEvent> async = gameEvents(runAsync(newGame()));

        assertEquals(EventLog.describe(sync), EventLog.describe(async),
                "The blocking and resume-based paths must produce the same gameplay events");
    }

    @Test
    void bothPaths_endWithOneWinnerAndConserveAllCards() {
        for (boolean async : new boolean[] {false, true}) {
            Game game = newGame();
            List<GameEvent> events = async ? runAsync(game) : runSync(game);
            assertEquals(1, events.stream().filter(GameEvent.GameEnded.class::isInstance).count(),
                    "GameEnded must fire exactly once (async=" + async + ")");
            assertNotNull(game.getWinner(), "a single winner must remain (async=" + async + ")");
            assertEquals(36, game.allCards().size(),
                    "all 36 cards must be accounted for at game end (async=" + async + ")");
        }
    }

    @Test
    void asyncPath_stillEmitsThePauseSignal() {
        // The equivalence above filters AwaitingHumanInput out; make sure the async
        // path really does pause (otherwise the test would compare two sync runs).
        List<GameEvent> async = runAsync(newGame());
        assertTrue(async.stream().anyMatch(GameEvent.AwaitingHumanInput.class::isInstance),
                "async path must emit AwaitingHumanInput at least once");
    }
}
