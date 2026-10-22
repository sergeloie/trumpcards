package ru.anseranser.desktop;

import com.badlogic.gdx.Gdx;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;

import java.util.ArrayList;
import java.util.List;

/**
 * The replacement of the old listener: the events of a transition become the
 * text of the log, and the state of the transition becomes the picture on the
 * screen.
 *
 * <p>The capture happens on the engine thread, where the state is consistent,
 * and the painting on the render thread: the widgets of LibGDX are not
 * thread-safe.</p>
 */
final class DesktopRenderer {

    /** The number of lines the log keeps. */
    private static final int LOG_LIMIT = 200;

    private final GameScreen screen;
    private final Card.Suit humanSeat;
    private final List<String> log = new ArrayList<>();
    private State state;

    DesktopRenderer(GameScreen screen, Card.Suit humanSeat) {
        this.screen = screen;
        this.humanSeat = humanSeat;
    }

    /** Renders what the transition reports. Called on the engine thread. */
    void render(Transition transition) {
        transition.events().forEach(event -> log.addAll(EventText.lines(event)));
        while (log.size() > LOG_LIMIT) {
            log.remove(0);
        }
        state = transition.state();
        repaint();
    }

    /**
     * Repaints the current state, for instance when the legal moves become
     * known and the screen has to mark the cards that may be played.
     */
    void repaint() {
        if (state == null) {
            return;
        }
        GameSnapshot snapshot = GameSnapshot.of(state, humanSeat, log);
        if (Gdx.app != null) {
            Gdx.app.postRunnable(() -> screen.render(snapshot));
        } else {
            screen.render(snapshot);
        }
    }
}
