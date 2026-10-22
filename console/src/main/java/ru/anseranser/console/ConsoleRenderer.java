package ru.anseranser.console;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Event;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.i18n.Messages;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the events of a transition as text. The engine decides what happens
 * in the game; this class only says it.
 *
 * <p>The hands and the boards of a round are printed with the round, taken
 * from the state of the transition: the events report the cards that were
 * dealt, not the hand a seat ends the round with.</p>
 */
public final class ConsoleRenderer {

    private final PrintStream out;
    private final Messages messages;
    private final CardLocalizer cards;

    public ConsoleRenderer(PrintStream out) {
        this(out, new Messages(), new CardLocalizer());
    }

    public ConsoleRenderer(PrintStream out, Messages messages) {
        this(out, messages, new CardLocalizer(messages));
    }

    public ConsoleRenderer(PrintStream out, Messages messages, CardLocalizer cards) {
        this.out = out;
        this.messages = messages;
        this.cards = cards;
    }

    /** Prints what the transition reports. */
    public void render(Transition transition) {
        for (Event event : transition.events()) {
            for (String line : lines(event, transition.state())) {
                out.println(line);
            }
        }
    }

    private List<String> lines(Event event, State state) {
        return switch (event) {
            case Event.GameStarted e -> List.of(messages.get("game.started"));
            case Event.RoundStarted e -> roundLines(e, state);
            case Event.CardsDealt e -> List.of();
            case Event.CardsExchanged e -> List.of(messages.get("event.card_exchanged",
                    cards.suitName(e.from().trump()), cards.cardName(e.card()),
                    cards.suitName(e.to().trump())));
            case Event.CardPlayed e -> List.of(messages.get("event.card_played",
                    cards.suitName(e.seat().trump()), cards.cardName(e.card())));
            case Event.CardBeaten e -> List.of(messages.get("event.card_beaten",
                    cards.suitName(e.seat().trump()), cards.cardName(e.attacking()),
                    cards.cardName(e.beating())));
            case Event.PotTaken e -> List.of(messages.get("event.pot_taken",
                    cards.suitName(e.seat().trump()), cards.cardName(e.topCard()), e.potSize()));
            case Event.RoundEnded e -> roundEndLines(e, state);
            case Event.SeatEliminated e -> List.of(
                    messages.get("round.eliminated", cards.suitName(e.seat().trump())));
            case Event.GameEnded e -> List.of(messages.get("game.ended"),
                    messages.get("game.winner", cards.suitName(e.winner().trump())));
        };
    }

    private List<String> roundLines(Event.RoundStarted event, State state) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.get("round.header"));
        lines.add(messages.get("round.dealer", cards.suitName(event.dealer().trump())));
        lines.addAll(scoreboardLines(state));
        for (Seat seat : state.seats()) {
            lines.add(messages.get("round.hand", cards.suitName(seat.trump()), handText(seat.hand())));
        }
        return lines;
    }

    private List<String> roundEndLines(Event.RoundEnded event, State state) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.get("round.end"));
        lines.add(messages.get("round.loser", cards.suitName(event.loser().trump())));
        if (event.pushedToBoard().isPresent()) {
            lines.add(messages.get("scoreboard.push", cards.suitName(event.loser().trump()),
                    cards.cardName(event.pushedToBoard().get())));
            lines.addAll(scoreboardLines(state));
        }
        return lines;
    }

    private List<String> scoreboardLines(State state) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.get("scoreboard.header"));
        for (Seat seat : state.seats()) {
            lines.add("  " + cards.suitName(seat.trump()) + ": " + handText(seat.board()));
        }
        return lines;
    }

    private String handText(List<Card> hand) {
        List<String> names = new ArrayList<>();
        for (Card card : hand) {
            names.add(cards.cardName(card));
        }
        return String.join(", ", names);
    }
}
