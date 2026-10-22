package ru.anseranser.desktop;

import ru.anseranser.engine.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * The text of an event, in plain ASCII: the default LibGDX font has no glyph
 * beyond ASCII and a non-ASCII character in a label throws.
 *
 * <p>The engine decides what happens in the game; this class only says it.</p>
 */
final class EventText {

    private EventText() {
    }

    /** The lines an event is shown as. */
    static List<String> lines(Event event) {
        return switch (event) {
            case Event.GameStarted e -> List.of("Game started.");
            case Event.RoundStarted e -> List.of("Round started. Dealer: " + e.dealer().trump());
            case Event.CardsDealt e -> List.of();
            case Event.CardsExchanged e -> List.of(e.from().trump() + " gives "
                    + CardCodes.of(e.card()) + " to " + e.to().trump());
            case Event.CardPlayed e -> List.of(e.seat().trump() + " plays " + CardCodes.of(e.card()));
            case Event.CardBeaten e -> List.of(e.seat().trump() + " beats "
                    + CardCodes.of(e.attacking()) + " with " + CardCodes.of(e.beating()));
            case Event.PotTaken e -> List.of(e.seat().trump() + " takes the pot ("
                    + e.potSize() + " cards)");
            case Event.RoundEnded e -> roundEnded(e);
            case Event.SeatEliminated e -> List.of(e.seat().trump() + " is eliminated");
            case Event.GameEnded e -> List.of("Game over. Winner: " + e.winner().trump());
        };
    }

    private static List<String> roundEnded(Event.RoundEnded event) {
        List<String> lines = new ArrayList<>();
        lines.add("End of round. Loser: " + event.loser().trump());
        event.pushedToBoard().ifPresent(card -> lines.add(event.loser().trump()
                + " adds " + CardCodes.of(card) + " to the scoreboard"));
        return lines;
    }
}
