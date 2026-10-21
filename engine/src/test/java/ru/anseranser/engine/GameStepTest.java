package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * One step of the game: the card played or the pot taken, the turn that
 * follows it and the counter that keeps the round from running forever.
 */
class GameStepTest {

    @Test
    void aLeadEndsTheTurnAndCountsIt() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));
        Seat diamonds = seat(DIAMONDS, List.of(card(DIAMONDS, NINE)));

        State stepped = Game.step(state(List.of(spades, clubs, diamonds),
                List.of(), spades, false, 0), 0).state();

        assertEquals(List.of(card(SPADES, SEVEN)), stepped.pot());
        assertEquals(List.of(), stepped.seats().get(0).hand());
        assertSame(stepped.seats().get(1), stepped.turn(), "the turn passes to the next seat");
        assertEquals(1, stepped.turns(), "the lead ended the turn");
        assertFalse(stepped.leadRequired());
    }

    @Test
    void aBeatCountsNoTurnAndTheSameSeatLeadsTheNextTrick() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT), card(CLUBS, TEN)));

        State stepped = Game.step(state(List.of(spades, clubs),
                List.of(card(SPADES, ACE)), clubs, false, 1), 0).state();

        assertEquals(List.of(card(SPADES, ACE), card(CLUBS, EIGHT)), stepped.pot());
        assertSame(stepped.seats().get(1), stepped.turn(), "the seat that beat leads the next trick");
        assertTrue(stepped.leadRequired(), "the seat that has just beaten has to lead");
        assertEquals(1, stepped.turns(), "the turn is not over yet, the seat leads the next trick");
    }

    @Test
    void aBeatThatEmptiesTheHandCountsTheTurnAndPassesTheTurnOn() {
        Seat spades = seat(SPADES, List.of());
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));
        Seat diamonds = seat(DIAMONDS, List.of(card(DIAMONDS, NINE)));
        Seat hearts = seat(HEARTS, List.of(card(HEARTS, TEN)));

        State stepped = Game.step(state(List.of(spades, clubs, diamonds, hearts),
                List.of(card(SPADES, SEVEN)), clubs, false, 1), 0).state();

        assertEquals(List.of(), stepped.seats().get(1).hand());
        assertSame(stepped.seats().get(2), stepped.turn(), "the seat with an empty hand is out of the round");
        assertEquals(2, stepped.turns(), "the beat emptied the hand, the turn is over");
        assertFalse(stepped.leadRequired());
    }

    @Test
    void takingThePotCountsTheTurnAndPassesTheTurnOn() {
        Seat spades = seat(SPADES, List.of(card(CLUBS, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        State stepped = Game.step(state(List.of(spades, clubs),
                List.of(card(SPADES, ACE)), spades, false, 1), 0).state();

        assertEquals(List.of(), stepped.pot(), "the pot is taken whole");
        assertEquals(List.of(card(CLUBS, SEVEN), card(SPADES, ACE)), stepped.seats().get(0).hand(),
                "the pot comes into the hand of the seat that took it");
        assertSame(stepped.seats().get(1), stepped.turn());
        assertEquals(2, stepped.turns());
        assertFalse(stepped.leadRequired());
    }

    @Test
    void theTurnPassesToTheNextSeatThatHoldsCards() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of());
        Seat diamonds = seat(DIAMONDS, List.of(card(DIAMONDS, NINE)));
        Seat hearts = seat(HEARTS, List.of(card(HEARTS, TEN)));

        State stepped = Game.step(state(List.of(spades, clubs, diamonds, hearts),
                List.of(), spades, false, 0), 0).state();

        assertSame(stepped.seats().get(2), stepped.turn(), "a seat with an empty hand is skipped");
    }

    @Test
    void aStepThatEndsTheRoundResolvesItAndDealsTheNextOne() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, SEVEN)));

        State stepped = Game.step(state(List.of(spades, clubs),
                List.of(), spades, false, 0), 0).state();

        assertEquals(List.of(card(CLUBS, SIX), card(CLUBS, SEVEN)),
                stepped.seats().get(1).board(), "the seat that still holds cards loses the round");
        assertEquals(List.of(), stepped.deck(), "the cards that came back are dealt again");
        assertEquals(List.of(), stepped.pot());
        assertEquals(0, stepped.turns(), "the counter of the new round starts at zero");
        assertSame(stepped.seats().get(0), stepped.turn(),
                "the dealer of the new round holds no card, the turn goes to the seat that can act");
    }

    @Test
    void theRoundEndsWhenTheTurnCounterReachesItsBound() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, NINE), card(CLUBS, TEN)));
        Seat diamonds = seat(DIAMONDS, List.of(card(DIAMONDS, TEN)));

        State stepped = Game.step(state(List.of(spades, clubs, diamonds),
                List.of(), spades, false, 9_999), 0).state();

        assertEquals(0, stepped.turns(), "the counter of the new round starts at zero");
        assertEquals(List.of(card(CLUBS, SIX), card(CLUBS, NINE)), stepped.seats().get(1).board(),
                "the seat with the most cards loses the capped round");
    }

    @Test
    void anIndexThatIsNotAMoveIsRejected() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        State state = state(List.of(spades, clubs), List.of(), spades, false, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> Game.step(state, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Game.step(state, -1));
    }

    @Test
    void aStepOfAGameThatIsOverIsRejected() {
        Seat winner = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat loser = new Seat(CLUBS, List.of(), List.of(card(CLUBS, SIX)), true);

        State state = state(List.of(winner, loser), List.of(), winner, false, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> Game.step(state, 0));
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }

    private static Seat seat(Card.Suit trump, List<Card> hand) {
        return new Seat(trump, hand, List.of(card(trump, SIX)), false);
    }

    private static State state(List<Seat> seats, List<Card> pot, Seat turn, boolean leadRequired,
            int turns) {
        return new State(seats, pot, List.of(), seats.get(0), turn, leadRequired, turns, 42L);
    }
}
