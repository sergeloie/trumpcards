package ru.anseranser.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The obligatory exchange: every card of another seat's trump suit that equals
 * the next rank required by that seat's board is passed to that seat.
 *
 * <p>The ace is never passed, and the transfers are collected first and applied
 * afterwards, so a card that arrives during the exchange is not passed on.
 */
final class Exchange {

    private Exchange() {
    }

    /**
     * The state after the exchange.
     *
     * <p>The transfers are collected in seating order and, within a seat, in
     * the order of its hand. They are applied in that order: a card leaves the
     * hand that holds it and is appended to the end of the hand of the seat
     * that owns its suit.
     *
     * <p>The transition reports every card that was passed and the seat it
     * went to.
     */
    static Transition apply(State state) {
        List<Seat> seats = state.seats();
        int seatCount = seats.size();

        Card.Rank[] required = new Card.Rank[seatCount];
        for (int i = 0; i < seatCount; i++) {
            required[i] = Ladders.nextRequiredRank(seats.get(i).board());
        }

        List<Transfer> transfers = new ArrayList<>();
        for (int from = 0; from < seatCount; from++) {
            Seat seat = seats.get(from);
            for (Card card : seat.hand()) {
                if (card.suit() == seat.trump()) {
                    continue;
                }
                int to = Seats.indexOfTrump(seats, card.suit());
                Card.Rank wanted = required[to];
                if (wanted == null || wanted == Card.Rank.ACE || card.rank() != wanted) {
                    continue;
                }
                transfers.add(new Transfer(from, card, to));
            }
        }

        List<List<Card>> hands = new ArrayList<>();
        for (Seat seat : seats) {
            hands.add(new ArrayList<>(seat.hand()));
        }
        for (Transfer transfer : transfers) {
            hands.get(transfer.from()).remove(transfer.card());
            hands.get(transfer.to()).add(transfer.card());
        }

        List<Seat> exchanged = new ArrayList<>();
        for (int i = 0; i < seatCount; i++) {
            Seat seat = seats.get(i);
            exchanged.add(new Seat(seat.trump(), hands.get(i), seat.board(), seat.out()));
        }
        State exchangedState = new State(exchanged, state.pot(), state.deck(),
                exchanged.get(Seats.indexOfTrump(seats, state.dealer().trump())),
                exchanged.get(Seats.indexOfTrump(seats, state.turn().trump())),
                state.leadRequired(), state.turns(), state.rngState());

        List<Event> events = new ArrayList<>();
        for (Transfer transfer : transfers) {
            events.add(new Event.CardsExchanged(exchanged.get(transfer.from()),
                    exchanged.get(transfer.to()), transfer.card()));
        }
        return new Transition(exchangedState, events);
    }

    private record Transfer(int from, Card card, int to) {
    }
}
