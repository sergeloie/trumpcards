package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.QUEEN;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Rank.THREE;
import static ru.anseranser.engine.Card.Rank.TWO;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

class ExchangeTest {

    @Test
    void theRankABoardRequiresNextGoesToTheSeatThatOwnsTheSuit() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(), exchanged.seats().get(0).hand(),
                "the seven of clubs belongs to the seat that plays clubs");
        assertEquals(List.of(card(CLUBS, SEVEN)), exchanged.seats().get(1).hand());
    }

    @Test
    void theRankIsCountedFromTheTopOfTheBoard() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, NINE), card(CLUBS, TEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX, SEVEN, EIGHT)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(CLUBS, TEN)), exchanged.seats().get(0).hand(),
                "only the nine is required, the ten stays where it is");
        assertEquals(List.of(card(CLUBS, NINE)), exchanged.seats().get(1).hand());
    }

    @Test
    void theRankAfterTheTopOfTheBoardIsRequired() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, THREE), card(CLUBS, SEVEN)), board(SPADES, TWO)),
                seat(CLUBS, List.of(), board(CLUBS, TWO)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(CLUBS, SEVEN)), exchanged.seats().get(0).hand(),
                "the board requires the rank after its top card, the three, not the seven");
        assertEquals(List.of(card(CLUBS, THREE)), exchanged.seats().get(1).hand());
    }

    @Test
    void theAceIsNeverPassed() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, ACE)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX, SEVEN, EIGHT, NINE, TEN, KING)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(CLUBS, ACE)), exchanged.seats().get(0).hand(),
                "the ace stays with the seat that holds it");
        assertEquals(List.of(), exchanged.seats().get(1).hand());
    }

    @Test
    void aCompleteBoardRequiresNothing() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, ACE)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX, SEVEN, EIGHT, NINE, TEN, KING, ACE)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(CLUBS, ACE)), exchanged.seats().get(0).hand());
        assertEquals(List.of(), exchanged.seats().get(1).hand());
    }

    @Test
    void aCardThatArrivesDuringTheExchangeIsNotPassedOn() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(card(SPADES, SEVEN)), board(CLUBS, SIX)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(SPADES, SEVEN)), exchanged.seats().get(0).hand(),
                "the seven of spades arrives at its owner and is not passed on");
        assertEquals(List.of(card(CLUBS, SEVEN)), exchanged.seats().get(1).hand(),
                "the seven of clubs arrives at its owner and is not passed on");
    }

    @Test
    void thePassedCardIsAppendedToTheEndOfTheReceiversHand() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, SEVEN), card(CLUBS, EIGHT), card(DIAMONDS, SEVEN)),
                        board(SPADES, SIX)),
                seat(CLUBS, List.of(card(SPADES, SEVEN)), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(CLUBS, EIGHT), card(SPADES, SEVEN)), exchanged.seats().get(0).hand(),
                "the cards that stay keep their order, the card that arrives goes last");
        assertEquals(List.of(card(CLUBS, SEVEN)), exchanged.seats().get(1).hand());
        assertEquals(List.of(card(DIAMONDS, SEVEN)), exchanged.seats().get(2).hand());
    }

    @Test
    void theExchangeKeepsTheNumberOfCards() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, SEVEN), card(CLUBS, EIGHT), card(HEARTS, SEVEN)),
                        board(SPADES, SIX)),
                seat(CLUBS, List.of(card(SPADES, SEVEN), card(HEARTS, ACE)), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State exchanged = Exchange.apply(state);

        assertEquals(cardsInPlay(state), cardsInPlay(exchanged));
        assertEquals(cardsInHands(state), cardsInHands(exchanged));
        assertEquals(5, cardsInHands(exchanged));
    }

    @Test
    void theBoardsAreNotTouched() {
        State state = state(
                seat(SPADES, List.of(card(CLUBS, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)));

        State exchanged = Exchange.apply(state);

        assertEquals(List.of(card(SPADES, SIX)), exchanged.seats().get(0).board());
        assertEquals(List.of(card(CLUBS, SIX)), exchanged.seats().get(1).board());
    }

    @Test
    void theDealerAndTheSeatToMoveAreTheExchangedSeats() {
        Seat spades = seat(SPADES, List.of(card(CLUBS, SEVEN)), board(SPADES, SIX));
        Seat clubs = seat(CLUBS, List.of(), board(CLUBS, SIX));
        State state = new State(List.of(spades, clubs), List.of(), List.of(), spades, clubs, 7L);

        State exchanged = Exchange.apply(state);

        assertSame(exchanged.seats().get(0), exchanged.dealer());
        assertSame(exchanged.seats().get(1), exchanged.turn());
        assertEquals(7L, exchanged.rngState());
    }

    @Test
    void theExchangeRunsWhenTheGameStarts() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS, HEARTS), 42L)).state();

        assertEquals(List.of(
                card(SPADES, EIGHT), card(CLUBS, QUEEN), card(CLUBS, TEN), card(CLUBS, ACE),
                card(HEARTS, JACK), card(HEARTS, ACE), card(SPADES, SEVEN)),
                state.seats().get(0).hand());
        assertEquals(List.of(
                card(SPADES, JACK), card(DIAMONDS, ACE), card(HEARTS, QUEEN), card(SPADES, NINE),
                card(HEARTS, EIGHT), card(SPADES, KING), card(DIAMONDS, TEN), card(DIAMONDS, JACK),
                card(CLUBS, SEVEN)),
                state.seats().get(1).hand());
        assertEquals(List.of(
                card(CLUBS, NINE), card(CLUBS, EIGHT), card(CLUBS, KING), card(SPADES, ACE),
                card(HEARTS, TEN), card(CLUBS, JACK), card(DIAMONDS, EIGHT), card(SPADES, QUEEN),
                card(DIAMONDS, SEVEN)),
                state.seats().get(2).hand());
        assertEquals(List.of(
                card(HEARTS, NINE), card(DIAMONDS, NINE), card(DIAMONDS, QUEEN), card(HEARTS, SEVEN),
                card(HEARTS, KING), card(DIAMONDS, KING), card(SPADES, TEN)),
                state.seats().get(3).hand());
    }

    @Test
    void everySeatHoldsTheNextRankOfItsOwnTrump() {
        State four = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS, HEARTS), 42L)).state();
        State three = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS), 42L)).state();
        State two = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS), 42L)).state();

        for (State state : List.of(four, three, two)) {
            for (Seat seat : state.seats()) {
                assertEquals(1, seat.hand().stream()
                                .filter(card -> card.suit() == seat.trump() && card.rank() == SEVEN)
                                .count(),
                        "the seven of " + seat.trump() + " belongs to the seat that plays it");
                for (Card.Suit other : List.of(SPADES, CLUBS, DIAMONDS, HEARTS)) {
                    if (other == seat.trump()) {
                        continue;
                    }
                    assertEquals(0, seat.hand().stream()
                                    .filter(card -> card.suit() == other && card.rank() == SEVEN)
                                    .count(),
                            "no seat keeps the seven of " + other);
                }
                assertEquals(List.of(card(seat.trump(), SIX)), seat.board(),
                        "the board of a seat keeps the base card of its own trump");
            }
        }
    }

    @Test
    void aTwoPlayerTableExchangesTheSuitsInPlay() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, List.of(SPADES, CLUBS), 42L, List.of(
                card(CLUBS, SEVEN), card(SPADES, SEVEN),
                card(SPADES, EIGHT), card(CLUBS, EIGHT),
                card(SPADES, NINE), card(CLUBS, NINE),
                card(SPADES, TEN), card(CLUBS, TEN),
                card(SPADES, JACK), card(CLUBS, JACK),
                card(SPADES, QUEEN), card(CLUBS, QUEEN),
                card(SPADES, KING), card(CLUBS, KING),
                card(SPADES, ACE), card(CLUBS, ACE)));

        State state = Game.start(setup).state();

        assertEquals(List.of(
                card(SPADES, EIGHT), card(SPADES, NINE), card(SPADES, TEN), card(SPADES, JACK),
                card(SPADES, QUEEN), card(SPADES, KING), card(SPADES, ACE), card(SPADES, SEVEN)),
                state.seats().get(0).hand());
        assertEquals(List.of(
                card(CLUBS, EIGHT), card(CLUBS, NINE), card(CLUBS, TEN), card(CLUBS, JACK),
                card(CLUBS, QUEEN), card(CLUBS, KING), card(CLUBS, ACE), card(CLUBS, SEVEN)),
                state.seats().get(1).hand());
    }

    @Test
    void aThreePlayerTableExchangesTheSuitsInPlay() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS), 42L)).state();

        assertEquals(27, RankSet.SIX_TO_ACE.cardCount(3));
        assertEquals(27, cardsInPlay(state));
        assertEquals(3, state.seats().size());
        for (Seat seat : state.seats()) {
            assertEquals(1, seat.hand().stream()
                            .filter(card -> card.suit() == seat.trump() && card.rank() == SEVEN)
                            .count(),
                    "the seven of " + seat.trump() + " belongs to the seat that plays it");
            assertEquals(List.of(card(seat.trump(), SIX)), seat.board());
        }
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }

    private static List<Card> board(Card.Suit suit, Card.Rank... ranks) {
        List<Card> cards = new ArrayList<>();
        for (Card.Rank rank : ranks) {
            cards.add(new Card(suit, rank));
        }
        return cards;
    }

    private static Seat seat(Card.Suit trump, List<Card> hand, List<Card> board) {
        return new Seat(trump, hand, board, false);
    }

    private static State state(Seat... seats) {
        List<Seat> all = List.of(seats);
        return new State(all, List.of(), List.of(), all.get(0), all.get(0), 0L);
    }

    private static int cardsInPlay(State state) {
        int total = state.pot().size() + state.deck().size();
        for (Seat seat : state.seats()) {
            total += seat.hand().size() + seat.board().size();
        }
        return total;
    }

    private static int cardsInHands(State state) {
        int total = 0;
        for (Seat seat : state.seats()) {
            total += seat.hand().size();
        }
        return total;
    }
}
