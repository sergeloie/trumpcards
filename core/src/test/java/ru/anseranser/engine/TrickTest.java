package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.QUEEN;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * What the seat to move may do: lead, beat or take the pot, and the order the
 * moves come in.
 */
class TrickTest {

    @Test
    void onAFreshTrickEveryCardOfTheHandIsLegalInTheOrderOfTheHand() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN), card(CLUBS, QUEEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        List<Move> moves = Game.legalMoves(
                state(List.of(spades, clubs), List.of(), spades, false, 0));

        assertEquals(List.of(new Move.Play(card(SPADES, SEVEN)), new Move.Play(card(CLUBS, QUEEN))),
                moves, "a fresh trick is led with any card of the hand");
    }

    @Test
    void aSeatThatHasJustBeatLeadsTheNextTrickWithAnyCardOfItsHand() {
        Seat spades = seat(SPADES, List.of(card(CLUBS, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        List<Move> moves = Game.legalMoves(state(List.of(spades, clubs),
                List.of(card(SPADES, ACE)), spades, true, 1));

        assertEquals(List.of(new Move.Play(card(CLUBS, SEVEN))), moves,
                "the seat that has just beaten leads, even with a card that cannot beat");
    }

    @Test
    void onlyTheCardsThatBeatTheTopCardAreLegal() {
        Seat clubs = seat(CLUBS, List.of(card(HEARTS, NINE), card(HEARTS, JACK),
                card(CLUBS, EIGHT), card(SPADES, TEN)));
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));

        List<Move> moves = Game.legalMoves(state(List.of(clubs, spades),
                List.of(card(HEARTS, SEVEN)), clubs, false, 1));

        assertEquals(List.of(new Move.Play(card(HEARTS, NINE)), new Move.Play(card(HEARTS, JACK)),
                new Move.Play(card(CLUBS, EIGHT))), moves,
                "the ten of spades is neither the trump of the seat nor of the suit of the top card");
    }

    @Test
    void theDefencesComeInTheOrderOfIsTrumpAndRank() {
        Seat hearts = seat(HEARTS, List.of(card(HEARTS, TEN), card(HEARTS, EIGHT),
                card(SPADES, TEN), card(DIAMONDS, TEN)));
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));

        List<Move> moves = Game.legalMoves(state(List.of(hearts, spades),
                List.of(card(SPADES, SEVEN)), hearts, false, 1));

        assertEquals(List.of(new Move.Play(card(SPADES, TEN)), new Move.Play(card(HEARTS, EIGHT)),
                new Move.Play(card(HEARTS, TEN))), moves,
                "the card that is not the trump comes first, the weakest trump before the stronger");
    }

    @Test
    void thePotIsTakenWhenNoCardCanBeatTheTopCard() {
        Seat spades = seat(SPADES, List.of(card(CLUBS, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        List<Move> moves = Game.legalMoves(state(List.of(spades, clubs),
                List.of(card(SPADES, ACE)), spades, false, 1));

        assertEquals(List.of(new Move.TakePot()), moves, "nothing beats the ace of spades");
    }

    @Test
    void thereAreNoLegalMovesWhenOnlyOneSeatIsLeftInTheGame() {
        Seat winner = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat loser = eliminated(CLUBS, List.of(card(CLUBS, SIX)));

        assertEquals(List.of(), Game.legalMoves(
                state(List.of(winner, loser), List.of(), winner, false, 0)));
    }

    @Test
    void aCardOfTheSameSuitBeatsTheTopCardWhenItIsHigher() {
        assertTrue(Trick.canBeat(CLUBS, card(CLUBS, SEVEN), card(CLUBS, EIGHT)));
        assertFalse(Trick.canBeat(CLUBS, card(CLUBS, EIGHT), card(CLUBS, SEVEN)));
        assertFalse(Trick.canBeat(CLUBS, card(CLUBS, SEVEN), card(CLUBS, SEVEN)));
    }

    @Test
    void theTrumpOfTheSeatBeatsACardOfAnotherSuit() {
        assertTrue(Trick.canBeat(SPADES, card(CLUBS, ACE), card(SPADES, SEVEN)));
        assertFalse(Trick.canBeat(SPADES, card(CLUBS, ACE), card(HEARTS, ACE)),
                "the trump of another seat beats nothing");
    }

    @Test
    void aTrumpIsBeatenOnlyByAHigherTrumpOfTheSameSuit() {
        assertTrue(Trick.canBeat(SPADES, card(SPADES, SEVEN), card(SPADES, EIGHT)));
        assertFalse(Trick.canBeat(SPADES, card(SPADES, TEN), card(SPADES, SEVEN)),
                "the seven of spades is lower than the ten of spades");
        assertFalse(Trick.canBeat(SPADES, card(SPADES, SEVEN), card(HEARTS, ACE)),
                "a card of another suit beats a trump only when it is the trump of the seat");
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }

    private static Seat seat(Card.Suit trump, List<Card> hand) {
        return new Seat(trump, hand, List.of(card(trump, SIX)), false);
    }

    private static Seat eliminated(Card.Suit trump, List<Card> board) {
        return new Seat(trump, List.of(), board, true);
    }

    private static State state(List<Seat> seats, List<Card> pot, Seat turn, boolean leadRequired,
            int turns) {
        return new State(seats, pot, List.of(), seats.get(0), turn, leadRequired, turns, 42L);
    }
}
