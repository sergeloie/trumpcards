package ru.anseranser.desktop;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The desktop's view of the game: the state of the engine, the moves the seat
 * to move may choose from, the text of the log and the status line.
 *
 * <p>The state of the engine is already immutable, so the snapshot only adds
 * what the screen needs. It is captured on the engine thread, where the state
 * is consistent, and painted on the render thread.</p>
 *
 * <p>The seat the person plays is supplied by the composition root
 * ({@link DesktopLauncher}) rather than assumed here, so the view stays free of
 * the "SPADES is the person" convention.</p>
 */
record GameSnapshot(
        State state,
        Card.Suit humanSeat,
        List<Move> legalMoves,
        String status,
        List<String> logLines) {

    GameSnapshot {
        legalMoves = List.copyOf(legalMoves);
        logLines = List.copyOf(logLines);
    }

    /** The snapshot of a state, with the legal moves the engine gives for it. */
    static GameSnapshot of(State state, Card.Suit humanSeat, List<String> logLines) {
        return new GameSnapshot(state, humanSeat, Game.legalMoves(state),
                status(state, humanSeat), logLines);
    }

    /** Where an opponent sits relative to the person (bottom). */
    enum SeatPosition {
        LEFT, TOP, RIGHT
    }

    /** One opponent's summary for the side panels. */
    record OpponentView(SeatPosition position, Card.Suit trump, int cardCount) {}

    /** The cards in the pot, in the order they were played. */
    List<Card> pot() {
        return state.pot();
    }

    /** The hand of the seat the person plays. */
    List<Card> humanHand() {
        for (Seat seat : state.seats()) {
            if (seat.trump() == humanSeat) {
                return seat.hand();
            }
        }
        return List.of();
    }

    /**
     * The opponents (every seat but the person's) in the seating order. They
     * are placed LEFT, TOP and RIGHT in that order, around the person at the
     * bottom.
     */
    List<OpponentView> opponents() {
        SeatPosition[] positions = {SeatPosition.LEFT, SeatPosition.TOP, SeatPosition.RIGHT};
        List<OpponentView> views = new ArrayList<>();
        int i = 0;
        for (Seat seat : state.seats()) {
            if (seat.trump() != humanSeat && !seat.out()) {
                views.add(new OpponentView(positions[i % positions.length], seat.trump(), seat.hand().size()));
                i++;
            }
        }
        return views;
    }

    /** The ladder of every seat, bottom-up. */
    Map<Card.Suit, List<Card>> scoreboard() {
        Map<Card.Suit, List<Card>> boards = new EnumMap<>(Card.Suit.class);
        for (Seat seat : state.seats()) {
            boards.put(seat.trump(), seat.board());
        }
        return boards;
    }

    /** The cards of the hand that the seat to move may play. */
    Set<Card> playableCards() {
        Set<Card> cards = new LinkedHashSet<>();
        for (Move move : legalMoves) {
            if (move instanceof Move.Play play) {
                cards.add(play.card());
            }
        }
        return cards;
    }

    /** Whether the seat to move may take the pot. */
    boolean takePotAvailable() {
        for (Move move : legalMoves) {
            if (move instanceof Move.TakePot) {
                return true;
            }
        }
        return false;
    }

    private static String status(State state, Card.Suit humanSeat) {
        int left = 0;
        for (Seat seat : state.seats()) {
            if (!seat.out()) {
                left++;
            }
        }
        return "Trump: " + state.dealer().trump() + "   Players left: " + left;
    }
}
