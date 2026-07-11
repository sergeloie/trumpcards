package ru.anseranser.event;

import ru.anseranser.i18n.Messages;
import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Console rendering of {@link GameEvent}s. This is the only place (in the
 * console target) that turns game data into human-readable text.
 *
 * Introduced in refactor Stage 1: it relocates all {@code System.out} that used
 * to live inside the model into the presentation layer. Refactor Stage 6 moved
 * all user-facing strings into a {@link Messages} ResourceBundle, so this class
 * only wires event data to localized patterns and never hardcodes text.
 */
public class ConsoleGameListener implements GameListener {

    private final Messages messages;

    public ConsoleGameListener() {
        this(new Messages());
    }

    public ConsoleGameListener(Messages messages) {
        this.messages = messages;
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e ->
                    System.out.println(messages.get("game.started"));
            case GameEvent.RoundStarted e -> {
                System.out.println(messages.get("round.header"));
                System.out.println(messages.get("round.dealer", e.dealer()));
                printScoreboard(e.scoreboard());
                for (Map.Entry<Player, List<Card>> entry : e.hands().entrySet()) {
                    System.out.println(messages.get("round.hand", entry.getKey(), entry.getValue()));
                }
            }
            case GameEvent.CardPlayed e ->
                    System.out.println(messages.get("event.card_played", e.player(), e.card()));
            case GameEvent.CardBeaten e ->
                    System.out.println(messages.get("event.card_beaten", e.player(), e.attacking(), e.beating()));
            case GameEvent.PotTaken e ->
                    System.out.println(messages.get("event.pot_taken", e.player(), e.topCard(), e.potSize()));
            case GameEvent.RoundEnded e -> {
                System.out.println(messages.get("round.end"));
                System.out.println(messages.get("round.loser", e.loser()));
                if (e.pushedToScoreboard() != null) {
                    System.out.println(messages.get("scoreboard.push",
                            e.pushedToScoreboard().suit(), e.pushedToScoreboard()));
                    printScoreboard(e.scoreboard());
                }
                if (e.eliminated()) {
                    System.out.println(messages.get("round.eliminated", e.loser()));
                }
            }
            case GameEvent.GameEnded e -> {
                System.out.println(messages.get("game.ended"));
                System.out.println(messages.get("game.winner", e.winner()));
            }
        }
    }

    private void printScoreboard(Map<Card.Suit, List<Card>> scoreboard) {
        System.out.println(messages.get("scoreboard.header"));
        for (Map.Entry<Card.Suit, List<Card>> entry : scoreboard.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
