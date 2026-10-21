package ru.anseranser.ml;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.ArrayList;
import java.util.List;

/**
 * The table as the seat sees it.
 *
 * <p>Nothing that is hidden from the observer is in here: the observer's own
 * hand, the pot and the boards are public, and an opponent contributes its
 * card count, its suit and whether it is out of the game, never its cards.
 *
 * <p>The offsets are counted in seats from the observer, so the first seat
 * after the observer is one seat away and the observer itself is zero seats
 * away.
 */
public record Observation(
        Card.Suit trump,
        List<Card> hand,
        List<Card> pot,
        List<Opponent> opponents,
        List<Board> boards,
        int seatToMoveOffset,
        int dealerOffset,
        int seatCount,
        int legalMoveCount) {

    /** An opponent: what the table shows of it. */
    public record Opponent(Card.Suit suit, int cardCount, boolean out) {
    }

    /**
     * A board: the rank it requires next, and whether its ladder is complete.
     * The required rank is absent when the board requires none, which is the
     * case when the ladder is complete or when the suit is not in play.
     */
    public record Board(Card.Suit suit, Card.Rank requiredRank, boolean complete) {
    }

    public Observation {
        hand = List.copyOf(hand);
        pot = List.copyOf(pot);
        opponents = List.copyOf(opponents);
        boards = List.copyOf(boards);
    }

    /**
     * The state as the seat sees it. The legal moves are the ones handed in,
     * and the observation only counts them.
     *
     * @throws IllegalArgumentException when the seat is not at the table
     */
    public static Observation of(State state, Seat seat, List<Move> moves) {
        List<Seat> seats = state.seats();
        int observer = indexOf(seats, seat);
        int seatCount = seats.size();

        List<Opponent> opponents = new ArrayList<>();
        for (int offset = 1; offset < seatCount; offset++) {
            Seat other = seats.get((observer + offset) % seatCount);
            opponents.add(new Opponent(other.trump(), other.hand().size(), other.out()));
        }

        List<Board> boards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            Seat owner = owner(seats, suit);
            if (owner == null) {
                boards.add(new Board(suit, null, false));
                continue;
            }
            Card.Rank required = requiredRank(owner.board());
            boards.add(new Board(suit, required, !owner.board().isEmpty() && required == null));
        }

        return new Observation(seat.trump(), seat.hand(), state.pot(), opponents, boards,
                offset(seats, state.turn(), observer, seatCount),
                offset(seats, state.dealer(), observer, seatCount),
                seatCount, moves.size());
    }

    /** The index of the seat at the table. */
    private static int indexOf(List<Seat> seats, Seat seat) {
        int index = seats.indexOf(seat);
        if (index < 0) {
            throw new IllegalArgumentException("the seat that plays " + seat.trump()
                    + " is not at the table");
        }
        return index;
    }

    /** The seat that plays the suit, or none when the suit is not in play. */
    private static Seat owner(List<Seat> seats, Card.Suit suit) {
        for (Seat seat : seats) {
            if (seat.trump() == suit) {
                return seat;
            }
        }
        return null;
    }

    /** The distance in seats from the observer to the seat. */
    private static int offset(List<Seat> seats, Seat seat, int observer, int seatCount) {
        return (indexOf(seats, seat) - observer + seatCount) % seatCount;
    }

    /**
     * The rank the board requires next: the one after the top of the ladder,
     * and none when the ladder is complete. The engine applies the same rule
     * to the ladder it keeps to itself.
     */
    private static Card.Rank requiredRank(List<Card> board) {
        if (board.isEmpty()) {
            return null;
        }
        Card.Rank[] ranks = Card.Rank.values();
        int next = board.get(board.size() - 1).rank().ordinal() + 1;
        return next >= ranks.length ? null : ranks[next];
    }
}
