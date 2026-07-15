package ru.anseranser.desktop;

import ru.anseranser.model.Card;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable copy of the game state the UI needs, captured on the engine
 * (game-loop) thread and then handed to the LibGDX render thread.
 *
 * <p>LibGDX scene2d is not thread-safe: widgets must only be mutated from the
 * render thread. The engine, however, runs on a dedicated background thread and
 * fires events from there. To avoid a data race on both the scene graph and the
 * live {@link Game} model, we copy everything the view needs here (on the engine
 * thread, where the model is consistent) and let the render thread consume the
 * copy without ever touching the live {@link Game}.</p>
 */
record GameSnapshot(
        String status,
        Map<Card.Suit, List<Card>> scoreboard,
        List<OpponentView> opponents,
        List<Card> pot,
        List<Card> humanHand,
        List<String> logLines) {

    /** One opponent's summary for the top-right panel. */
    record OpponentView(String trump, int cardCount) {}

    /** Capture a consistent snapshot of {@code game} on the calling (engine) thread. */
    static GameSnapshot capture(Game game, List<String> log) {
        String status = "Trump: " + game.getTrump() + "   Players left: " + countGamers(game);

        Map<Card.Suit, List<Card>> sb = new LinkedHashMap<>();
        game.getScoreboard().snapshot().forEach((suit, stack) -> sb.put(suit, new ArrayList<>(stack)));

        List<OpponentView> opponents = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            if (p.isGamer() && p.getTrump() != Card.Suit.SPADES) {
                opponents.add(new OpponentView(p.getTrump().name(), p.getHand().size()));
            }
        }

        List<Card> pot = new ArrayList<>(game.getPot());

        List<Card> humanHand = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            if (p.getTrump() == Card.Suit.SPADES && p.isGamer()) {
                humanHand.addAll(p.getHand());
                break;
            }
        }

        return new GameSnapshot(status, sb, opponents, pot, humanHand, new ArrayList<>(log));
    }

    private static int countGamers(Game game) {
        int n = 0;
        for (Player p : game.getPlayers()) {
            if (p.isGamer()) n++;
        }
        return n;
    }
}
