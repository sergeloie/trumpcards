package ru.anseranser.verify;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;
import ru.anseranser.exporter.Policy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The checks of the invariants: the cards of the deck are in exactly one
 * place, the game ends with exactly one seat left in it, and a policy never
 * chooses a move that is not legal.
 */
class CheckerTest {

    @Test
    void theCheckCatchesACardThatIsLost() {
        Seat seat = new Seat(SPADES, List.of(card(SPADES, SEVEN)), List.of(), false);
        State state = state(List.of(seat));

        InvariantViolation violation =
                assertThrows(InvariantViolation.class, () -> Checker.check(state, 2, 42L, 7));

        assertTrue(violation.getMessage().contains("seed 42"), violation.getMessage());
        assertTrue(violation.getMessage().contains("step 7"), violation.getMessage());
        assertTrue(violation.getMessage().contains("the cards in play"), violation.getMessage());
    }

    @Test
    void theCheckCatchesACardThatIsDuplicated() {
        Seat first = new Seat(SPADES, List.of(card(SPADES, SEVEN)), List.of(), false);
        Seat second = new Seat(CLUBS, List.of(card(SPADES, SEVEN)), List.of(), false);
        State state = state(List.of(first, second));

        InvariantViolation violation =
                assertThrows(InvariantViolation.class, () -> Checker.check(state, 2, 42L, 7));

        assertTrue(violation.getMessage().contains("the cards in play"), violation.getMessage());
    }

    @Test
    void theCheckCatchesTwoSeatsLeftInTheGame() {
        Seat first = new Seat(SPADES, List.of(), List.of(), false);
        Seat second = new Seat(CLUBS, List.of(), List.of(), false);
        State state = state(List.of(first, second));

        InvariantViolation violation =
                assertThrows(InvariantViolation.class, () -> Checker.winner(state, 42L, 7));

        assertTrue(violation.getMessage().contains("2 seats left in it"), violation.getMessage());
        assertTrue(violation.getMessage().contains("seed 42"), violation.getMessage());
    }

    @Test
    void theCheckerCatchesAPolicyThatChoosesAnIllegalMove() {
        Policy breaking = (state, moves) -> moves.size();
        Setup setup = new Setup(RankSet.SIX_TO_ACE, List.of(SPADES, CLUBS), 42L);

        InvariantViolation violation =
                assertThrows(InvariantViolation.class, () -> Checker.play(setup, breaking));

        assertTrue(violation.getMessage().contains("seed 42"), violation.getMessage());
        assertTrue(violation.getMessage().contains("the policy chose the move"), violation.getMessage());
    }

    @Test
    void theCheckerPlaysAGameAndReportsWhatItLeftBehind() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, List.of(SPADES, CLUBS), 42L);

        Checker.Result game = Checker.play(setup, (state, moves) -> 0);

        assertEquals(42L, game.seed());
        assertTrue(game.steps() > 0, "the game was played");
        assertTrue(game.rounds() >= 1, "the game played at least one round");
        assertEquals(0, game.cappedRounds(), "the round did not hit the safety bound");
    }

    private static State state(List<Seat> seats) {
        return new State(seats, List.of(), List.of(), seats.get(0), seats.get(0), false, 0, 0L);
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
