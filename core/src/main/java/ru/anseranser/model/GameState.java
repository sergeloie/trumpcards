package ru.anseranser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable, presentation-agnostic snapshot of a {@link Game} at a point in time.
 *
 * <p>This is the single read API that every UI (desktop, mobile, web) should
 * consume instead of poking at {@link Game}'s live getters. It is built on the
 * engine thread (where the model is consistent) and then handed to a render
 * thread, so the view never touches the live model and can never mutate it:
 * every collection returned here is an unmodifiable copy.</p>
 *
 * <p>UI-specific decorations (a human-readable status line, a move log, etc.)
 * are intentionally <em>not</em> part of this class — they belong to the
 * presentation layer. The desktop port, for example, wraps a {@code GameState}
 * together with its own log in {@code GameSnapshot}.</p>
 *
 * @param trump     the trump suit of the current deal (the dealer's own suit)
 * @param scoreboard immutable per-suit ladders (top of each ladder first)
 * @param players   immutable view of every seat (trump, still-in-game flag,
 *                  whether it is controlled by a human, and an immutable hand)
 * @param pot       immutable copy of the cards currently in the middle
 */
public record GameState(
        Card.Suit trump,
        Map<Card.Suit, List<Card>> scoreboard,
        List<PlayerState> players,
        List<Card> pot) {

    /**
     * One seat's readable state. Pure data — no behaviour, no domain coupling.
     *
     * @param trump the seat's fixed trump suit (also its stable identity)
     * @param gamer {@code true} while the seat is still in the game
     * @param human {@code true} if the seat is driven by a human {@code InputProvider}
     * @param hand  immutable copy of the cards currently in the seat's hand
     */
    public record PlayerState(Card.Suit trump, boolean gamer, boolean human, List<Card> hand) {}

    /** Capture a consistent, immutable snapshot of {@code game}. */
    public static GameState of(Game game) {
        Map<Card.Suit, List<Card>> scoreboard = copyScoreboard(game.getScoreboard());

        List<PlayerState> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            players.add(new PlayerState(
                    p.getTrump(),
                    p.isGamer(),
                    p.isHuman(),
                    List.copyOf(p.getHand())));
        }

        return new GameState(
                game.getTrump(),
                scoreboard,
                List.copyOf(players),
                List.copyOf(game.getPot()));
    }

    private static Map<Card.Suit, List<Card>> copyScoreboard(Map<Card.Suit, List<Card>> source) {
        var copy = new java.util.LinkedHashMap<Card.Suit, List<Card>>();
        source.forEach((suit, stack) -> copy.put(suit, List.copyOf(stack)));
        return java.util.Map.copyOf(copy);
    }
}
