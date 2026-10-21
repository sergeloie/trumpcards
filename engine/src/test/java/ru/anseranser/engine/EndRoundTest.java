package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
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
 * The end of a round: the card that goes onto the loser's board, the cards
 * that come back to the deck, the elimination, the next round and the end of
 * the game.
 */
class EndRoundTest {

    @Test
    void theTrumpOfTheRankTheBoardRequiresGoesOntoTheLosersBoard() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, SEVEN), card(CLUBS, EIGHT)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(SPADES, SIX), card(SPADES, SEVEN)), ended.seats().get(0).board(),
                "the seven of spades is the rank the board requires next");
    }

    @Test
    void theLowestTrumpGoesOntoTheBoardWhenThePileHoldsNoCardOfTheRequiredRank() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, NINE), card(CLUBS, EIGHT)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(SPADES, SIX), card(SPADES, NINE)), ended.seats().get(0).board(),
                "the pile holds no seven of spades, so the lowest trump of the suit goes onto the board");
    }

    @Test
    void theLoserIsTheFirstSeatInSeatingOrderThatStillHoldsCards() {
        State state = state(
                seat(SPADES, List.of(), board(SPADES, SIX)),
                seat(CLUBS, List.of(card(CLUBS, SEVEN)), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(CLUBS, SIX), card(CLUBS, SEVEN)), ended.seats().get(1).board(),
                "the seat that still holds cards loses the round");
        assertEquals(List.of(card(SPADES, SIX)), ended.seats().get(0).board(),
                "the seat with an empty hand keeps its board");
    }

    @Test
    void theLoserOfACappedRoundIsTheSeatWithTheMostCards() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, NINE)), board(SPADES, SIX)),
                seat(CLUBS, List.of(card(CLUBS, EIGHT), card(CLUBS, NINE), card(CLUBS, TEN)),
                        board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(card(DIAMONDS, EIGHT)), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(CLUBS, SIX), card(CLUBS, EIGHT)), ended.seats().get(1).board(),
                "the seat with the most cards loses the capped round");
        assertEquals(List.of(card(SPADES, SIX)), ended.seats().get(0).board());
        assertEquals(List.of(card(DIAMONDS, SIX)), ended.seats().get(2).board());
    }

    @Test
    void aTieInTheNumberOfCardsIsBrokenByTheSeatingOrder() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, NINE)), board(SPADES, SIX)),
                seat(CLUBS, List.of(card(CLUBS, NINE)), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(SPADES, SIX), card(SPADES, NINE)), ended.seats().get(0).board(),
                "the first seat of the tie loses the round");
        assertEquals(List.of(card(CLUBS, SIX)), ended.seats().get(1).board());
    }

    @Test
    void theCardsThatCameBackAreDealtAgainInTheNextRound() {
        State state = state(List.of(card(HEARTS, NINE)),
                seat(SPADES, List.of(card(SPADES, SEVEN), card(SPADES, EIGHT)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(SPADES, EIGHT), card(HEARTS, NINE)), cardsInHands(ended),
                "the loser's cards and the pot are dealt again");
        assertEquals(List.of(), ended.deck(), "the deck is empty once the next round is dealt");
        assertEquals(List.of(), ended.pot(), "the pot is empty");
    }

    @Test
    void aCompleteBoardEliminatesItsSeat() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, ACE)),
                        board(SPADES, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertTrue(ended.seats().get(0).out(), "the seat whose board reached the ace is eliminated");
        assertEquals(List.of(card(SPADES, SIX), card(SPADES, SEVEN), card(SPADES, EIGHT),
                card(SPADES, NINE), card(SPADES, TEN), card(SPADES, JACK), card(SPADES, QUEEN),
                card(SPADES, KING), card(SPADES, ACE)), ended.seats().get(0).board());
        assertFalse(ended.seats().get(1).out());
    }

    @Test
    void theDealerOfTheNextRoundIsTheLoser() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertSame(ended.seats().get(0), ended.dealer(), "the loser deals the next round");
    }

    @Test
    void theDealerOfTheNextRoundIsTheNextSeatInTheGameWhenTheLoserIsEliminated() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, ACE)),
                        board(SPADES, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertSame(ended.seats().get(1), ended.dealer(),
                "the eliminated loser cannot deal, the next seat in the game does");
    }

    @Test
    void theDealerOfTheNextRoundLeadsIt() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertSame(ended.dealer(), ended.turn(), "the dealer leads the next round");
    }

    @Test
    void theNextRoundRunsTheObligatoryExchange() {
        State state = state(
                seat(SPADES, List.of(card(SPADES, SEVEN), card(HEARTS, SEVEN)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(List.of(card(HEARTS, SEVEN)), ended.seats().get(3).hand(),
                "the seven of hearts goes to the seat that plays hearts");
        assertEquals(List.of(card(SPADES, SIX), card(SPADES, SEVEN)), ended.seats().get(0).board());
    }

    @Test
    void theGameEndsWhenOneSeatIsLeftInIt() {
        State state = state(List.of(card(DIAMONDS, SEVEN), card(HEARTS, EIGHT)),
                eliminated(SPADES, board(SPADES, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE)),
                seat(CLUBS, List.of(card(CLUBS, ACE)),
                        board(CLUBS, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(1, Seats.countInGame(ended.seats()), "one seat is left in the game");
        assertFalse(ended.seats().get(2).out(), "the last seat standing wins");
        assertTrue(ended.seats().get(1).out(), "the loser was eliminated");
        assertEquals(List.of(card(DIAMONDS, SEVEN), card(HEARTS, EIGHT)), ended.deck(),
                "the cards that came back stay in the deck");
        assertEquals(List.of(), ended.pot());
        assertSame(ended.seats().get(2), ended.turn());
    }

    @Test
    void theRoundEndKeepsEveryCard() {
        State state = state(List.of(card(HEARTS, NINE)),
                seat(SPADES, List.of(card(SPADES, SEVEN), card(SPADES, EIGHT)), board(SPADES, SIX)),
                seat(CLUBS, List.of(), board(CLUBS, SIX)),
                seat(DIAMONDS, List.of(), board(DIAMONDS, SIX)),
                seat(HEARTS, List.of(), board(HEARTS, SIX)));

        State ended = EndRound.apply(state);

        assertEquals(cardsInPlay(state), cardsInPlay(ended));
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

    private static Seat eliminated(Card.Suit trump, List<Card> board) {
        return new Seat(trump, List.of(), board, true);
    }

    private static State state(Seat... seats) {
        return state(List.of(), seats);
    }

    private static State state(List<Card> pot, Seat... seats) {
        List<Seat> all = List.of(seats);
        return new State(all, pot, List.of(), all.get(0), all.get(0), false, 0, 42L);
    }

    private static List<Card> cardsInHands(State state) {
        return state.seats().stream()
                .flatMap(seat -> seat.hand().stream())
                .sorted(Comparator.comparing(Card::suit).thenComparing(Card::rank))
                .toList();
    }

    private static int cardsInPlay(State state) {
        int total = state.pot().size() + state.deck().size();
        for (Seat seat : state.seats()) {
            total += seat.hand().size() + seat.board().size();
        }
        return total;
    }
}
