package ru.anseranser.desktop;

import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
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
        screen.refresh(game);
        screen.setLog(log());
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.GameStarted s) {
            record("Game started.");
            repaint();
        } else if (event instanceof GameEvent.RoundStarted s) {
            StringBuilder sb = new StringBuilder("Round started. Hands: ");
            for (Player p : game.getPlayers()) {
                if (p.isGamer()) sb.append(p).append("=").append(p.getHand().size()).append(" ");
            }
            record(sb.toString());
            repaint();
        } else if (event instanceof GameEvent.CardPlayed e) {
            record(e.player() + " plays " + e.card());
            repaint();
        } else if (event instanceof GameEvent.CardBeaten e) {
            record(e.player() + " beats " + e.attacking() + " with " + e.beating());
            repaint();
        } else if (event instanceof GameEvent.PotTaken e) {
            record(e.player() + " takes the pot (" + e.potSize() + " cards)");
            repaint();
        } else if (event instanceof GameEvent.RoundEnded e) {
            if (e.pushedToScoreboard() != null) {
                record(e.loser() + " adds " + e.pushedToScoreboard() + " to the scoreboard");
            }
            if (e.eliminated()) {
                record(e.loser() + " is eliminated (ladder complete)");
            }
            repaint();
        } else if (event instanceof GameEvent.GameEnded e) {
            record("Game over. Winner: " + e.winner());
            repaint();
        }
    }

    /** Exposed for callers that want a fresh log line after a manual action. */
    void recordExternal(String line) {
        record(line);
        repaint();
    }
}
