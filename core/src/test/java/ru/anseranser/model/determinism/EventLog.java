package ru.anseranser.model.determinism;

import ru.anseranser.event.GameEvent;
import ru.anseranser.model.Card;

import java.util.List;
import java.util.Map;

/**
 * Canonical, order-stable rendering of a {@link GameEvent} stream, for comparing
 * two runs for bit-for-bit equality.
 *
 * <p>Why not use {@code GameEvent.equals}? Some events carry a {@link Map} keyed
 * by an enum ({@code Card.Suit}). Enum {@code hashCode} is identity-based, so a
 * {@link java.util.HashMap}'s iteration order is <em>not</em> stable across JVM
 * runs. Rendering the map sorted by the suits' declaration order makes the
 * comparison independent of that, so a genuine divergence is never masked and a
 * benign reordering never fails the test.</p>
 */
final class EventLog {

    private EventLog() {
    }

    static List<String> describe(List<GameEvent> events) {
        return events.stream().map(EventLog::describe).toList();
    }

    static String describe(GameEvent event) {
        if (event instanceof GameEvent.GameStarted) {
            return "GameStarted";
        }
        if (event instanceof GameEvent.AwaitingHumanInput e) {
            return "Awaiting " + e.player();
        }
        if (event instanceof GameEvent.RoundStarted e) {
            return "RoundStarted dealer=" + e.dealer() + scoreboard(e.scoreboard()) + hands(e.hands());
        }
        if (event instanceof GameEvent.CardPlayed e) {
            return "Played " + e.player() + " " + e.card();
        }
        if (event instanceof GameEvent.CardBeaten e) {
            return "Beaten " + e.player() + " " + e.attacking() + "<" + e.beating();
        }
        if (event instanceof GameEvent.PotTaken e) {
            return "TookPot " + e.player() + " top=" + e.topCard() + " size=" + e.potSize();
        }
        if (event instanceof GameEvent.RoundEnded e) {
            return "RoundEnded loser=" + e.loser() + " pushed=" + e.pushedToScoreboard()
                    + " eliminated=" + e.eliminated() + scoreboard(e.scoreboard());
        }
        if (event instanceof GameEvent.GameEnded e) {
            return "GameEnded winner=" + e.winner();
        }
        return event.toString();
    }

    private static String scoreboard(Map<Card.Suit, List<Card>> bySuit) {
        return render("sb", bySuit);
    }

    private static String hands(Map<Card.Suit, List<Card>> bySuit) {
        return render("hands", bySuit);
    }

    private static String render(String label, Map<Card.Suit, List<Card>> bySuit) {
        StringBuilder sb = new StringBuilder(" ").append(label).append('[');
        for (Card.Suit suit : Card.Suit.values()) {
            sb.append(suit).append('=').append(bySuit.getOrDefault(suit, List.of())).append(';');
        }
        return sb.append(']').toString();
    }
}
