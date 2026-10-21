package ru.anseranser.engine;

import ru.anseranser.event.GameEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The trace of a game: the events of one game, reduced to the facts both
 * engines report, so that two traces can be compared entry by entry.
 *
 * <p>The two streams differ in three ways, and the trace keeps what they
 * share: the old engine reports neither the deal nor the exchange, folds the
 * elimination into the end of the round, and asks a human for input; the new
 * engine reports the deal and the exchange, reports the elimination on its
 * own, and knows nothing of human input.
 */
final class Trace {

    private Trace() {
    }

    /** The trace of a game of the old engine. */
    static List<String> ofOld(List<GameEvent> events) {
        List<String> trace = new ArrayList<>();
        for (GameEvent event : events) {
            if (event instanceof GameEvent.GameStarted) {
                trace.add("started");
            } else if (event instanceof GameEvent.RoundStarted started) {
                trace.add("round " + started.dealer());
            } else if (event instanceof GameEvent.CardPlayed played) {
                trace.add("played " + played.player() + " " + card(played.card()));
            } else if (event instanceof GameEvent.CardBeaten beaten) {
                trace.add("beaten " + beaten.player() + " " + card(beaten.attacking())
                        + " over " + card(beaten.beating()));
            } else if (event instanceof GameEvent.PotTaken taken) {
                trace.add("pot " + taken.player() + " " + card(taken.topCard()) + " " + taken.potSize());
            } else if (event instanceof GameEvent.RoundEnded ended) {
                trace.add("round ended " + ended.loser() + " "
                        + (ended.pushedToScoreboard() == null ? "none" : card(ended.pushedToScoreboard()))
                        + " " + (ended.eliminated() ? "out" : "in"));
            } else if (event instanceof GameEvent.GameEnded ended) {
                trace.add("game ended " + ended.winner());
            }
        }
        return trace;
    }

    /** The trace of a game of the new engine. */
    static List<String> ofNew(List<Event> events) {
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (event instanceof Event.GameStarted) {
                trace.add("started");
            } else if (event instanceof Event.RoundStarted started) {
                trace.add("round " + started.dealer().trump());
            } else if (event instanceof Event.CardPlayed played) {
                trace.add("played " + played.seat().trump() + " " + card(played.card()));
            } else if (event instanceof Event.CardBeaten beaten) {
                trace.add("beaten " + beaten.seat().trump() + " " + card(beaten.attacking())
                        + " over " + card(beaten.beating()));
            } else if (event instanceof Event.PotTaken taken) {
                trace.add("pot " + taken.seat().trump() + " " + card(taken.topCard()) + " " + taken.potSize());
            } else if (event instanceof Event.RoundEnded ended) {
                trace.add("round ended " + ended.loser().trump() + " "
                        + (ended.pushedToBoard().isEmpty() ? "none" : card(ended.pushedToBoard().get()))
                        + " " + (eliminated(events, i, ended) ? "out" : "in"));
            } else if (event instanceof Event.GameEnded ended) {
                trace.add("game ended " + ended.winner().trump());
            }
        }
        return trace;
    }

    /** Whether the seat that lost the round left the game with it. */
    private static boolean eliminated(List<Event> events, int index, Event.RoundEnded ended) {
        if (index + 1 >= events.size()) {
            return false;
        }
        return events.get(index + 1) instanceof Event.SeatEliminated eliminated
                && eliminated.seat().trump() == ended.loser().trump();
    }

    private static String card(ru.anseranser.model.Card card) {
        return card.suit() + " " + card.rank();
    }

    private static String card(Card card) {
        return card.suit() + " " + card.rank();
    }
}
