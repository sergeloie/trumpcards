package ru.anseranser.desktop;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSnapshotTest {

    private static final Card SEVEN = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
    private static final Card ACE = new Card(Card.Suit.SPADES, Card.Rank.ACE);
    private static final Card HEART_SEVEN = new Card(Card.Suit.HEARTS, Card.Rank.SEVEN);
    private static final Card HEART_ACE = new Card(Card.Suit.HEARTS, Card.Rank.ACE);

    private static final Seat SPADES = new Seat(Card.Suit.SPADES, List.of(SEVEN, ACE), List.of(), false);
    private static final Seat CLUBS = new Seat(Card.Suit.CLUBS, List.of(), List.of(), false);
    private static final Seat HEARTS = new Seat(Card.Suit.HEARTS, List.of(HEART_SEVEN), List.of(), false);

    private static final State LEADING =
            new State(List.of(SPADES, CLUBS, HEARTS), List.of(), List.of(), SPADES, SPADES, false, 0, 0L);

    @Test
    void thePlayableCardsAreTheCardsAmongTheLegalMoves() {
        GameSnapshot snapshot = GameSnapshot.of(LEADING, Card.Suit.SPADES, List.of());

        assertEquals(Set.of(SEVEN, ACE), snapshot.playableCards());
        assertFalse(snapshot.takePotAvailable());
    }

    @Test
    void takingThePotIsOfferedWhenItIsTheOnlyLegalMove() {
        Seat weak = new Seat(Card.Suit.SPADES, List.of(HEART_SEVEN), List.of(), false);
        State defending = new State(List.of(weak, CLUBS, HEARTS),
                List.of(HEART_ACE), List.of(), weak, weak, false, 0, 0L);

        GameSnapshot snapshot = GameSnapshot.of(defending, Card.Suit.SPADES, List.of());

        assertTrue(snapshot.takePotAvailable());
        assertEquals(Set.of(), snapshot.playableCards());
    }

    @Test
    void theStatusHoldsTheTrumpOfTheDealerAndTheSeatsLeft() {
        GameSnapshot snapshot = GameSnapshot.of(LEADING, Card.Suit.SPADES, List.of());

        assertEquals("Trump: SPADES   Players left: 3", snapshot.status());
    }

    @Test
    void theHandOfThePersonIsTheHandOfItsSeat() {
        GameSnapshot snapshot = GameSnapshot.of(LEADING, Card.Suit.SPADES, List.of());

        assertEquals(List.of(SEVEN, ACE), snapshot.humanHand());
    }

    @Test
    void theOpponentsSitAroundThePerson() {
        GameSnapshot snapshot = GameSnapshot.of(LEADING, Card.Suit.SPADES, List.of());

        assertEquals(List.of(
                        new GameSnapshot.OpponentView(GameSnapshot.SeatPosition.LEFT, Card.Suit.CLUBS, 0),
                        new GameSnapshot.OpponentView(GameSnapshot.SeatPosition.TOP, Card.Suit.HEARTS, 1)),
                snapshot.opponents());
    }

    @Test
    void theScoreboardHoldsTheLadderOfEverySeat() {
        Seat withBoard = new Seat(Card.Suit.SPADES, List.of(), List.of(SEVEN, ACE), false);
        State state = new State(List.of(withBoard, CLUBS, HEARTS), List.of(), List.of(),
                withBoard, withBoard, false, 0, 0L);

        GameSnapshot snapshot = GameSnapshot.of(state, Card.Suit.SPADES, List.of());

        assertEquals(List.of(SEVEN, ACE), snapshot.scoreboard().get(Card.Suit.SPADES));
    }

    @Test
    void theLegalMovesAreTheOnesOfTheEngine() {
        GameSnapshot snapshot = GameSnapshot.of(LEADING, Card.Suit.SPADES, List.of());

        assertEquals(List.of(new Move.Play(SEVEN), new Move.Play(ACE)), snapshot.legalMoves());
    }
}
