package ru.anseranser.engine;

import java.util.List;
import java.util.Optional;

/**
 * Something that happened on the way from one state to the next. The happenings
 * are reported once and are not kept.
 */
public sealed interface Event permits
        Event.GameStarted,
        Event.RoundStarted,
        Event.CardsDealt,
        Event.CardsExchanged,
        Event.CardPlayed,
        Event.CardBeaten,
        Event.PotTaken,
        Event.RoundEnded,
        Event.SeatEliminated,
        Event.GameEnded {

    /** The game began. */
    record GameStarted() implements Event {}

    /** A round began; the dealer deals it. */
    record RoundStarted(Seat dealer) implements Event {}

    /** The cards a seat received from the deal. */
    record CardsDealt(Seat seat, List<Card> cards) implements Event {
        public CardsDealt {
            cards = List.copyOf(cards);
        }
    }

    /** A card passed from one seat to another in the obligatory exchange. */
    record CardsExchanged(Seat from, Seat to, Card card) implements Event {}

    /** A seat played a card. */
    record CardPlayed(Seat seat, Card card) implements Event {}

    /** A seat beat the attacking card with another card. */
    record CardBeaten(Seat seat, Card attacking, Card beating) implements Event {}

    /** A seat could not beat the top card and took the whole pot. */
    record PotTaken(Seat seat, Card topCard, int potSize) implements Event {}

    /** A round ended with the losing seat; the pushed trump is absent when it had none left. */
    record RoundEnded(Seat loser, Optional<Card> pushedToBoard) implements Event {}

    /** A seat's board reached the ace, and the seat left the game. */
    record SeatEliminated(Seat seat) implements Event {}

    /** The game ended with the surviving seat. */
    record GameEnded(Seat winner) implements Event {}
}
