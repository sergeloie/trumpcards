package ru.anseranser.engine;

import java.util.List;

/**
 * Questions about the seating order: which seat plays which suit, which seats
 * are still in the game and who sits next to whom.
 */
final class Seats {

    private Seats() {
    }

    /** The index of the seat that plays the suit. */
    static int indexOfTrump(List<Seat> seats, Card.Suit trump) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).trump() == trump) {
                return i;
            }
        }
        throw new IllegalArgumentException("no seat at the table plays " + trump);
    }

    /** The number of seats that are still in the game. */
    static int countInGame(List<Seat> seats) {
        int count = 0;
        for (Seat seat : seats) {
            if (!seat.out()) {
                count++;
            }
        }
        return count;
    }

    /** The number of seats that are still in the game and hold at least one card. */
    static int countWithCards(List<Seat> seats) {
        int count = 0;
        for (Seat seat : seats) {
            if (!seat.out() && !seat.hand().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** The index of the next seat still in the game after the seat at {@code index}. */
    static int nextInGame(List<Seat> seats, int index) {
        for (int i = 1; i <= seats.size(); i++) {
            int candidate = (index + i) % seats.size();
            if (!seats.get(candidate).out()) {
                return candidate;
            }
        }
        throw new IllegalStateException("no seat is still in the game");
    }

    /**
     * The index of the next seat that can act after the seat at {@code index}:
     * the next seat in seating order that is still in the game and holds at
     * least one card. A seat with an empty hand is out of the round.
     */
    static int nextWithCards(List<Seat> seats, int index) {
        for (int i = 1; i <= seats.size(); i++) {
            int candidate = (index + i) % seats.size();
            Seat seat = seats.get(candidate);
            if (!seat.out() && !seat.hand().isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("no seat can act");
    }
}
