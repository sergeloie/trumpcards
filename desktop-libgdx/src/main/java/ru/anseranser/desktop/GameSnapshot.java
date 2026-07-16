package ru.anseranser.desktop;

import ru.anseranser.model.Card;
import ru.anseranser.model.Game;
import ru.anseranser.model.GameState;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Desktop view-model: a core {@link GameState} (the immutable, presentation-agnostic
 * snapshot) plus the desktop-only decorations (a human-readable status line and the
 * rolling move log). The engine data itself lives in {@link GameState}; this class
 * only adds what the LibGDX screen needs and never reaches back into the live
 * {@link Game}.
 *
 * <p>LibGDX scene2d is not thread-safe: widgets must only be mutated from the render
 * thread. The engine runs on a dedicated background thread and fires events from
 * there, so we capture the {@link GameState} on the engine thread (where the model is
 * consistent) and hand the copy to the render thread via {@code Gdx.app.postRunnable}.</p>
 */
record GameSnapshot(GameState state, String status, List<String> logLines) {

    /** Where an opponent sits relative to the human (SPADES, bottom). */
    enum Seat {
        LEFT, TOP, RIGHT
    }

    /** One opponent's summary for the side panels. */
    record OpponentView(Seat seat, Card.Suit trump, int cardCount) {}

    /** Capture a consistent snapshot of {@code game} on the calling (engine) thread. */
    static GameSnapshot capture(Game game, List<String> log) {
        String status = "Trump: " + game.getTrump() + "   Players left: " + countGamers(game);
        return new GameSnapshot(GameState.of(game), status, new ArrayList<>(log));
    }

    // --- Accessors used by GameScreen (delegating to the immutable GameState) ---

    java.util.Map<Card.Suit, List<Card>> scoreboard() { return state.scoreboard(); }

    List<Card> pot() { return state.pot(); }

    /**
     * Opponents (every gamer except the human seat SPADES), in turn order. They
     * are placed LEFT, TOP, RIGHT in that order — matching the clockwise seating
     * around the human (SPADES) at the bottom.
     */
    List<OpponentView> opponents() {
        Seat[] seats = {Seat.LEFT, Seat.TOP, Seat.RIGHT};
        List<OpponentView> result = new ArrayList<>();
        int i = 0;
        for (GameState.PlayerState p : state.players()) {
            if (p.gamer() && p.trump() != Card.Suit.SPADES) {
                result.add(new OpponentView(seats[i % seats.length], p.trump(), p.hand().size()));
                i++;
            }
        }
        return result;
    }

    List<Card> humanHand() {
        List<Card> humanHand = new ArrayList<>();
        for (GameState.PlayerState p : state.players()) {
            if (p.trump() == Card.Suit.SPADES && p.gamer()) {
                humanHand.addAll(p.hand());
                break;
            }
        }
        return humanHand;
    }

    private static int countGamers(Game game) {
        int n = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) n++;
        }
        return n;
    }
}
