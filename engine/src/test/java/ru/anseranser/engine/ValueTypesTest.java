package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueTypesTest {

    private static final Card SIX_OF_SPADES = new Card(Card.Suit.SPADES, Card.Rank.SIX);

    @Test
    void setupCopiesAndFreezesItsTrumpSuits() {
        List<Card.Suit> suits = new ArrayList<>(List.of(Card.Suit.SPADES, Card.Suit.HEARTS));
        Setup setup = new Setup(RankSet.SIX_TO_ACE, suits, 42L);

        suits.add(Card.Suit.CLUBS);

        assertEquals(List.of(Card.Suit.SPADES, Card.Suit.HEARTS), setup.trumpSuits());
        assertThrows(UnsupportedOperationException.class, () -> setup.trumpSuits().add(Card.Suit.CLUBS));
    }

    @Test
    void setupRejectsATableItCannotDealFor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES, Card.Suit.SPADES), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES, Card.Suit.CLUBS,
                        Card.Suit.DIAMONDS, Card.Suit.HEARTS, Card.Suit.SPADES), 1L));
    }

    @Test
    void seatCopiesAndFreezesItsHandAndItsBoard() {
        List<Card> hand = new ArrayList<>(List.of(SIX_OF_SPADES));
        List<Card> board = new ArrayList<>();
        Seat seat = new Seat(Card.Suit.SPADES, hand, board, false);

        hand.clear();
        board.add(SIX_OF_SPADES);

        assertEquals(List.of(SIX_OF_SPADES), seat.hand());
        assertEquals(List.of(), seat.board());
        assertThrows(UnsupportedOperationException.class, () -> seat.hand().add(SIX_OF_SPADES));
        assertThrows(UnsupportedOperationException.class, () -> seat.board().add(SIX_OF_SPADES));
    }

    @Test
    void stateCopiesAndFreezesEveryCollectionItHolds() {
        List<Seat> seats = new ArrayList<>(List.of(seat()));
        List<Card> pot = new ArrayList<>(List.of(SIX_OF_SPADES));
        List<Card> deck = new ArrayList<>(List.of(SIX_OF_SPADES));
        State state = new State(seats, pot, deck, seats.get(0), seats.get(0), 7L);

        seats.clear();
        pot.clear();
        deck.clear();

        assertEquals(1, state.seats().size());
        assertEquals(List.of(SIX_OF_SPADES), state.pot());
        assertEquals(List.of(SIX_OF_SPADES), state.deck());
        assertThrows(UnsupportedOperationException.class, () -> state.seats().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.pot().add(SIX_OF_SPADES));
        assertThrows(UnsupportedOperationException.class, () -> state.deck().add(SIX_OF_SPADES));
    }

    @Test
    void transitionCopiesAndFreezesItsEvents() {
        List<Event> events = new ArrayList<>(List.of(new Event.GameStarted()));
        Transition transition = new Transition(state(), events);

        events.clear();

        assertEquals(1, transition.events().size());
        assertThrows(UnsupportedOperationException.class, () -> transition.events().clear());
    }

    @Test
    void cardsDealtEventCopiesAndFreezesItsCards() {
        List<Card> cards = new ArrayList<>(List.of(SIX_OF_SPADES));
        Event.CardsDealt dealt = new Event.CardsDealt(seat(), cards);

        cards.clear();

        assertEquals(List.of(SIX_OF_SPADES), dealt.cards());
        assertThrows(UnsupportedOperationException.class, () -> dealt.cards().add(SIX_OF_SPADES));
    }

    @Test
    void theValueTypesAreRecords() {
        List<Class<?>> types = List.of(Card.class, Setup.class, Seat.class, State.class,
                Transition.class, Move.Play.class, Move.TakePot.class, Event.GameStarted.class,
                Event.CardsDealt.class);
        for (Class<?> type : types) {
            assertTrue(type.isRecord(), type.getSimpleName() + " must be a record");
        }
    }

    @Test
    void aRankSetKnowsItsBaseRankAndItsRanks() {
        assertEquals(Card.Rank.SIX, RankSet.SIX_TO_ACE.baseRank());
        assertEquals(Card.Rank.TWO, RankSet.TWO_TO_ACE.baseRank());

        List<Card.Rank> ranks = RankSet.SIX_TO_ACE.ranks();
        assertEquals(Card.Rank.SIX, ranks.get(0));
        assertEquals(Card.Rank.ACE, ranks.get(ranks.size() - 1));
        assertTrue(RankSet.TWO_TO_ACE.ranks().size() > ranks.size());
    }

    private static Seat seat() {
        return new Seat(Card.Suit.SPADES, List.of(), List.of(), false);
    }

    private static State state() {
        Seat seat = seat();
        return new State(List.of(seat), List.of(), List.of(), seat, seat, 0L);
    }
}
