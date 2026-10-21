package ru.anseranser.ml;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The observation: the table as the seat that is about to move sees it.
 */
class ObservationTest {

    @Test
    void theObserverSeesItsOwnHandAndThePot() {
        Observation observation = observation(state());

        assertEquals(List.of(card(SPADES, SEVEN), card(CLUBS, NINE)), observation.hand(),
                "the observer sees its own cards");
        assertEquals(List.of(card(HEARTS, TEN)), observation.pot(), "the pot is public");
        assertEquals(SPADES, observation.trump());
    }

    @Test
    void anOpponentIsOnlyItsCountItsSuitAndItsFlag() {
        Observation observation = observation(state());

        assertEquals(List.of(
                new Observation.Opponent(CLUBS, 1, false),
                new Observation.Opponent(DIAMONDS, 1, false),
                new Observation.Opponent(HEARTS, 0, true)),
                observation.opponents(),
                "an opponent contributes a count, a suit and a flag, never its cards");
    }

    @Test
    void theCardsOfAnOpponentDoNotReachTheObservation() {
        Observation observation = observation(stateWith(new Seat(CLUBS, List.of(card(CLUBS, JACK)),
                List.of(card(CLUBS, SIX)), false)));

        assertEquals(observation(state()), observation,
                "the observation does not change when the cards of an opponent change");
    }

    @Test
    void theBoardsShowTheRankTheyRequireNext() {
        Observation observation = observation(state());

        assertEquals(List.of(
                new Observation.Board(SPADES, SEVEN, false),
                new Observation.Board(CLUBS, SEVEN, false),
                new Observation.Board(DIAMONDS, SEVEN, false),
                new Observation.Board(HEARTS, null, true)),
                observation.boards(),
                "a board shows the rank it requires next, and whether its ladder is complete");
    }

    @Test
    void theTableShowsTheSeatToMoveAndTheDealer() {
        Observation observation = observation(state());

        assertEquals(0, observation.seatToMoveOffset(), "the observer is the seat to move");
        assertEquals(1, observation.dealerOffset(), "the dealer is the next seat");
        assertEquals(4, observation.seatCount());
        assertEquals(2, observation.legalMoveCount());
    }

    @Test
    void theObservationIsAValue() {
        assertEquals(observation(state()), observation(state()),
                "two observations of the same state are the same value");
        assertEquals(observation(state()).hashCode(), observation(state()).hashCode());
    }

    @Test
    void theSeatThatLooksIsTheOneThatLooks() {
        Observation fromSpades = observation(state());
        Observation fromClubs = Observation.of(state(), clubs(), Game.legalMoves(state()));

        assertNotEquals(fromSpades, fromClubs);
        assertEquals(CLUBS, fromClubs.trump(), "the clubs seat sees its own cards");
        assertEquals(List.of(card(CLUBS, EIGHT)), fromClubs.hand());
        assertEquals(fromSpades.boards(), fromClubs.boards(), "the boards are public");
    }

    @Test
    void aSeatThatIsNotAtTheTableIsRejected() {
        Seat stranger = new Seat(HEARTS, List.of(), List.of(), false);

        assertThrows(IllegalArgumentException.class,
                () -> Observation.of(state(), stranger, Game.legalMoves(state())));
    }

    private static Observation observation(State state) {
        return Observation.of(state, state.turn(), Game.legalMoves(state));
    }

    /** A table of four seats: the observer holds two cards and is about to lead. */
    private static State state() {
        return new State(seats(), List.of(card(HEARTS, TEN)), List.of(), clubs(), spades(),
                true, 0, 42L);
    }

    private static State stateWith(Seat clubs) {
        return new State(List.of(spades(), clubs, diamonds(), hearts()),
                List.of(card(HEARTS, TEN)), List.of(), clubs, spades(), true, 0, 42L);
    }

    private static List<Seat> seats() {
        return List.of(spades(), clubs(), diamonds(), hearts());
    }

    private static Seat spades() {
        return seat(SPADES, List.of(card(SPADES, SEVEN), card(CLUBS, NINE)));
    }

    private static Seat clubs() {
        return seat(CLUBS, List.of(card(CLUBS, EIGHT)));
    }

    private static Seat diamonds() {
        return seat(DIAMONDS, List.of(card(DIAMONDS, KING)));
    }

    private static Seat hearts() {
        return new Seat(HEARTS, List.of(), List.of(card(HEARTS, SIX), card(HEARTS, ACE)), true);
    }

    private static Seat seat(Card.Suit trump, List<Card> hand) {
        return new Seat(trump, hand, List.of(card(trump, SIX)), false);
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
