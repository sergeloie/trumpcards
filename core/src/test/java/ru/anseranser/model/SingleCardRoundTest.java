package ru.anseranser.model;

import org.junit.jupiter.api.Test;
import ru.anseranser.event.GameEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A round that begins with a single card in play: the pool that came back
 * from the previous round is smaller than the number of seats, so a single
 * seat holds the only card of the round.
 *
 * <p>Such a round ends at once, with the seat that holds the card as its
 * loser. A round in which no seat holds a card at all has no loser and no
 * turn to advance to: the engine says so instead of looping forever.
 */
class SingleCardRoundTest {

    /** How long the engine may take before the test calls it a hang. */
    private static final long TIMEOUT_MILLIS = 5_000;

    @Test
    void aRoundThatBeginsWithASingleCardInPlayEndsAtOnce() {
        List<GameEvent> events = new ArrayList<>();
        Game game = new Game();
        game.setRng(new SplitMix64(42));
        game.setCardDealer(new SingleCardDealer(Layout.NEXT_TO_DEALER));
        game.setListener(events::add);

        Game.GameDriver driver = game.createDriver();
        driver.startGame();

        assertEquals(Game.Phase.ROUND_ENDED, driver.getPhase(),
                "a round that begins with a single card in play ends at once");
        assertEquals(1, driver.save().driver().moves(),
                "the empty turn of a dealer without cards counts as a turn of the round");
        assertFalse(driver.step(), "no card is played in a round that ends at once");

        driver.finishRound();

        Card.Suit holder = holderOf(first(events, GameEvent.RoundStarted.class));
        assertEquals(holder, first(events, GameEvent.RoundEnded.class).loser(),
                "the seat that holds the only card of the round loses it");
    }

    @Test
    void theEngineFailsLoudlyWhenTheDealerPlaysTheOnlyCardOfTheRound() {
        Game game = new Game();
        game.setRng(new SplitMix64(42));
        game.setCardDealer(new SingleCardDealer(Layout.DEALER));

        Outcome outcome = runBounded(() -> {
            Game.GameDriver driver = game.createDriver();
            driver.startGame();
            driver.step();
        });

        assertTrue(outcome.finished(), "the engine must not loop forever");
        assertInstanceOf(IllegalStateException.class, outcome.failure(),
                "the engine must fail loudly when no seat holds a card");
    }

    @Test
    void theEngineFailsLoudlyWhenNoSeatHoldsACard() {
        Game game = new Game();
        game.setRng(new SplitMix64(42));
        game.setCardDealer(new SingleCardDealer(Layout.NOBODY));

        Outcome outcome = runBounded(() -> {
            Game.GameDriver driver = game.createDriver();
            driver.startGame();
        });

        assertTrue(outcome.finished(), "the engine must not loop forever");
        assertInstanceOf(IllegalStateException.class, outcome.failure(),
                "the engine must fail loudly when no seat holds a card");
    }

    /** The seat that holds a card in the round that has just begun. */
    private static Card.Suit holderOf(GameEvent.RoundStarted started) {
        return started.hands().entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the round must begin with a card in play"));
    }

    private static <T> T first(List<GameEvent> events, Class<T> type) {
        return events.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the event stream has no " + type.getSimpleName()));
    }

    /**
     * Run the action on a daemon thread and wait for it: an endless loop in
     * the engine fails the test instead of hanging the build.
     */
    private static Outcome runBounded(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        }, "engine-under-test");
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(TIMEOUT_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return new Outcome(!worker.isAlive(), failure.get());
    }

    private record Outcome(boolean finished, Throwable failure) {}

    /** Where the single card of the round goes. */
    private enum Layout {
        /** The seat to the left of the dealer holds it, and the dealer holds none. */
        NEXT_TO_DEALER,
        /** The dealer holds it, and nobody else holds a card. */
        DEALER,
        /** Nobody holds a card. */
        NOBODY
    }

    /**
     * A dealer that hands out a single card, so that the round begins with a
     * single card in play (or with none at all).
     */
    private static final class SingleCardDealer implements CardDealer {

        private final Layout layout;
        private final List<Card> pool = new ArrayList<>();

        SingleCardDealer(Layout layout) {
            this.layout = layout;
        }

        @Override
        public List<Card> deck() {
            return pool;
        }

        @Override
        public List<Card> takeBaseCards(Card.Rank baseRank) {
            List<Card> base = new ArrayList<>();
            for (Card.Suit suit : Card.Suit.values()) {
                base.add(new Card(suit, baseRank));
            }
            return base;
        }

        @Override
        public void shuffle(Rng rng) {
        }

        @Override
        public void deal(TurnOrder order, Player dealer, Predicate<Player> active) {
            if (layout == Layout.NOBODY) {
                return;
            }
            Player holder = layout == Layout.DEALER ? dealer : order.nextActive(dealer, active);
            holder.addCard(new Card(dealer.getTrump(), Card.Rank.NINE));
        }

        @Override
        public void returnCards(Collection<Card> cards) {
            pool.addAll(cards);
        }

        @Override
        public void setDeck(Collection<Card> cards) {
            pool.clear();
            pool.addAll(cards);
        }
    }
}
