package ru.anseranser.desktop;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Event;
import ru.anseranser.engine.Seat;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTextTest {

    private static final Card SEVEN = new Card(Card.Suit.SPADES, Card.Rank.SEVEN);
    private static final Card SIX = new Card(Card.Suit.SPADES, Card.Rank.SIX);
    private static final Card KING = new Card(Card.Suit.HEARTS, Card.Rank.KING);

    @Test
    void rendersTheStartOfTheGame() {
        assertEquals(List.of("Game started."), EventText.lines(new Event.GameStarted()));
    }

    @Test
    void rendersTheStartOfTheRound() {
        assertEquals(List.of("Round started. Dealer: SPADES"),
                EventText.lines(new Event.RoundStarted(seat(Card.Suit.SPADES))));
    }

    @Test
    void rendersNothingForTheDealtCards() {
        assertEquals(List.of(), EventText.lines(new Event.CardsDealt(seat(Card.Suit.SPADES), List.of(SEVEN))));
    }

    @Test
    void rendersTheExchangedCard() {
        assertEquals(List.of("SPADES gives 7S to HEARTS"),
                EventText.lines(new Event.CardsExchanged(seat(Card.Suit.SPADES), seat(Card.Suit.HEARTS), SEVEN)));
    }

    @Test
    void rendersTheCardPlayed() {
        assertEquals(List.of("SPADES plays 7S"),
                EventText.lines(new Event.CardPlayed(seat(Card.Suit.SPADES), SEVEN)));
    }

    @Test
    void rendersTheCardBeaten() {
        assertEquals(List.of("HEARTS beats 7S with KH"),
                EventText.lines(new Event.CardBeaten(seat(Card.Suit.HEARTS), SEVEN, KING)));
    }

    @Test
    void rendersThePotTaken() {
        assertEquals(List.of("HEARTS takes the pot (3 cards)"),
                EventText.lines(new Event.PotTaken(seat(Card.Suit.HEARTS), SEVEN, 3)));
    }

    @Test
    void rendersTheEndOfTheRound() {
        assertEquals(List.of("End of round. Loser: SPADES", "SPADES adds 6S to the scoreboard"),
                EventText.lines(new Event.RoundEnded(seat(Card.Suit.SPADES), Optional.of(SIX))));
    }

    @Test
    void rendersTheEndOfARoundWithoutACardForTheBoard() {
        assertEquals(List.of("End of round. Loser: SPADES"),
                EventText.lines(new Event.RoundEnded(seat(Card.Suit.SPADES), Optional.empty())));
    }

    @Test
    void rendersTheEliminationOfTheSeat() {
        assertEquals(List.of("SPADES is eliminated"),
                EventText.lines(new Event.SeatEliminated(seat(Card.Suit.SPADES))));
    }

    @Test
    void rendersTheWinner() {
        assertEquals(List.of("Game over. Winner: HEARTS"),
                EventText.lines(new Event.GameEnded(seat(Card.Suit.HEARTS))));
    }

    private static Seat seat(Card.Suit trump) {
        return new Seat(trump, List.of(), List.of(), false);
    }
}
