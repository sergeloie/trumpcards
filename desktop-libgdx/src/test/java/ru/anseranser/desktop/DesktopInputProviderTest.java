package ru.anseranser.desktop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopInputProviderTest {

    private static final Card SEVEN = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
    private static final Card ACE = new Card(Card.Suit.SPADES, Card.Rank.ACE);
    private static final Card QUEEN = new Card(Card.Suit.HEARTS, Card.Rank.QUEEN);

    private static final List<Move> MOVES = List.of(
            new Move.Play(SEVEN),
            new Move.Play(ACE),
            new Move.TakePot());

    private static final Seat SPADES = new Seat(Card.Suit.SPADES, List.of(SEVEN, ACE), List.of(), false);
    private static final State STATE =
            new State(List.of(SPADES), List.of(), List.of(), SPADES, SPADES, false, 0, 0L);

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    @Test
    void clickOnACardResolvesTheIndexOfTheMoveThatPlaysIt() throws Exception {
        DesktopInputProvider provider = new DesktopInputProvider();
        Future<Integer> choice = choosing(provider);

        provider.onCardClicked(ACE);

        assertEquals(1, choice.get(5, TimeUnit.SECONDS));
    }

    @Test
    void clickOnTheTakePotButtonResolvesTheIndexOfThatMove() throws Exception {
        DesktopInputProvider provider = new DesktopInputProvider();
        Future<Integer> choice = choosing(provider);

        provider.onTakePotClicked();

        assertEquals(2, choice.get(5, TimeUnit.SECONDS));
    }

    @Test
    void clickOnACardThatIsNotLegalIsIgnored() throws Exception {
        DesktopInputProvider provider = new DesktopInputProvider();
        Future<Integer> choice = choosing(provider);

        provider.onCardClicked(QUEEN);

        assertThrows(TimeoutException.class, () -> choice.get(200, TimeUnit.MILLISECONDS));
        provider.onCardClicked(SEVEN);
        assertEquals(0, choice.get(5, TimeUnit.SECONDS));
    }

    @Test
    void theLegalMovesAreEmptyWhenNoChoiceIsPending() {
        DesktopInputProvider provider = new DesktopInputProvider();
        assertEquals(List.of(), provider.legalMoves());
    }

    private Future<Integer> choosing(DesktopInputProvider provider) throws InterruptedException {
        Future<Integer> choice = pool.submit(() -> provider.choose(STATE, MOVES));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!MOVES.equals(provider.legalMoves()) && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(MOVES, provider.legalMoves());
        return choice;
    }
}
