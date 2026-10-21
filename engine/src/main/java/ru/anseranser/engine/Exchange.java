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
     */
    static State apply(State state, RankSet rankSet) {
        List<Seat> seats = state.seats();
        int seatCount = seats.size();

        Card.Rank[] required = new Card.Rank[seatCount];
        for (int i = 0; i < seatCount; i++) {
            required[i] = nextRequiredRank(seats.get(i).board(), rankSet);
        }

        List<Transfer> transfers = new ArrayList<>();
        for (int from = 0; from < seatCount; from++) {
            Seat seat = seats.get(from);
            for (Card card : seat.hand()) {
                if (card.suit() == seat.trump()) {
                    continue;
                }
                int to = indexOfTrump(seats, card.suit());
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
        return new State(exchanged, state.pot(), state.deck(),
                exchanged.get(indexOfTrump(seats, state.dealer().trump())),
                exchanged.get(indexOfTrump(seats, state.turn().trump())),
                state.rngState());
    }

    /**
     * The rank the board requires next: the one after the top of the board,
     * within the rank set in play. It is absent when the board is complete.
     */
    private static Card.Rank nextRequiredRank(List<Card> board, RankSet rankSet) {
        if (board.isEmpty()) {
            return null;
        }
        List<Card.Rank> ranks = rankSet.ranks();
        int top = ranks.indexOf(board.get(board.size() - 1).rank());
        if (top < 0 || top + 1 >= ranks.size()) {
            return null;
        }
        return ranks.get(top + 1);
    }

    private static int indexOfTrump(List<Seat> seats, Card.Suit trump) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).trump() == trump) {
                return i;
            }
        }
        throw new IllegalArgumentException("no seat at the table plays " + trump);
    }

    private record Transfer(int from, Card card, int to) {
    }
}
