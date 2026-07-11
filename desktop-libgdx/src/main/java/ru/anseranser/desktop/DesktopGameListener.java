package ru.anseranser.desktop;

import com.badlogic.gdx.Gdx;
import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.model.Card;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * LibGDX implementation of {@link GameListener}: forwards engine events to the
 * {@link GameScreen} for rendering. It keeps a rolling log of move messages
 * (shown in the side text panel) and asks the screen to repaint on every event.
 *
 * <p>The engine is the single source of truth; this class only translates events
 * into view updates and never touches game rules.</p>
 */
public final class DesktopGameListener implements GameListener {

    /** Max lines kept in the move-log panel. */
    private static final int LOG_LIMIT = 200;

    private final List<String> log = new ArrayList<>();
    private final GameScreen screen;
    private final Game game;

    public DesktopGameListener(Game game, GameScreen screen) {
        this.game = game;
        this.screen = screen;
    }

    /** Current log lines, oldest first. */
    public List<String> log() {
        return new ArrayList<>(log);
    }

    private void record(String line) {
        log.add(line);
        if (log.size() > LOG_LIMIT) {
            log.remove(0);
        }
    }

    private void repaint() {
        // Capture a consistent snapshot on the engine (game-loop) thread, then
        // apply it on the LibGDX render thread. scene2d widgets are not thread-safe,
        // so mutating them from this background thread would race with stage.act()/draw().
        GameSnapshot snap = GameSnapshot.capture(game, log());
        if (Gdx.app != null) {
            Gdx.app.postRunnable(() -> screen.render(snap));
        } else {
            screen.render(snap);
        }
    }

    /** Repaint from the current game state. Safe to call from the engine thread
     *  (e.g. when the human's set of valid choices changes and no event fired). */
    public void requestRepaint() {
        repaint();
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted s) {
            record("Game started.");
            repaint();
        } else if (event instanceof GameEvent.RoundStarted s) {
            StringBuilder sb = new StringBuilder("Round started. Hands: ");
            for (Player p : game.getPlayers()) {
                if (p.isGamer()) sb.append(p.getTrump().name()).append("=").append(p.getHand().size()).append(" ");
            }
            record(sb.toString());
            repaint();
        } else if (event instanceof GameEvent.CardPlayed e) {
            record(seat(e.player()) + " plays " + card(e.card()));
            repaint();
        } else if (event instanceof GameEvent.CardBeaten e) {
            record(seat(e.player()) + " beats " + card(e.attacking()) + " with " + card(e.beating()));
            repaint();
        } else if (event instanceof GameEvent.PotTaken e) {
            record(seat(e.player()) + " takes the pot (" + e.potSize() + " cards)");
            repaint();
        } else if (event instanceof GameEvent.RoundEnded e) {
            if (e.pushedToScoreboard() != null) {
                record(seat(e.loser()) + " adds " + card(e.pushedToScoreboard()) + " to the scoreboard");
            }
            if (e.eliminated()) {
                record(seat(e.loser()) + " is eliminated (ladder complete)");
            }
            repaint();
        } else if (event instanceof GameEvent.GameEnded e) {
            record("Game over. Winner: " + seat(e.winner()));
            repaint();
        }
    }

    /** ASCII-safe seat name (the player's trump suit), e.g. "SPADES". */
    private String seat(Player p) {
        return p.getTrump().name();
    }

    /** ASCII-safe short card label, e.g. "AS", "7H". */
    private String card(Card c) {
        return new CardLocalizer(CardLocalizer.Style.LETTERS).cardName(c);
    }

    /** Exposed for callers that want a fresh log line after a manual action. */
    void recordExternal(String line) {
        record(line);
        repaint();
    }
}
