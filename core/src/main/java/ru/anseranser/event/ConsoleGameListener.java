package ru.anseranser.event;

import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.i18n.Messages;
import ru.anseranser.model.Card;

import java.util.List;
import java.util.Map;

/**
 * Console rendering of {@link GameEvent}s. This is the only place (in the
 * console target) that turns game data into human-readable text.
 *
 * <p>Introduced in refactor Stage 1: it relocates all {@code System.out} that used
 * to live inside the model into the presentation layer. Refactor Stage 6 moved
 * all user-facing strings into a {@link Messages} ResourceBundle, so this class
 * only wires event data to localized patterns and never hardcodes text.
 * Card names are localized through {@link CardLocalizer} (added after Stage 6),
 * so "ACE of SPADES" no longer leaks English into non-English consoles.</p>
 *
 * <p>Players are identified in events by their trump suit ({@link Card.Suit});
 * this listener renders that suit via {@link CardLocalizer#suitName}.</p>
 */
public class ConsoleGameListener implements GameListener {

    private final Messages messages;
    private final CardLocalizer cards;

    public ConsoleGameListener() {
        this(new Messages(), new CardLocalizer());
    }

    public ConsoleGameListener(Messages messages) {
        this(messages, new CardLocalizer(messages));
    }

    public ConsoleGameListener(Messages messages, CardLocalizer cards) {
        this.messages = messages;
        this.cards = cards;
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e ->
                    System.out.println(messages.get("game.started"));
            case GameEvent.AwaitingHumanInput e ->
                    System.out.println(messages.get("event.awaiting_input", cards.suitName(e.player())));
            case GameEvent.RoundStarted e -> {
                System.out.println(messages.get("round.header"));
                System.out.println(messages.get("round.dealer", cards.suitName(e.dealer())));
                printScoreboard(e.scoreboard());
                for (Map.Entry<Card.Suit, List<Card>> entry : e.hands().entrySet()) {
                    System.out.println(messages.get("round.hand", cards.suitName(entry.getKey()), handText(entry.getValue())));
                }
            }
            case GameEvent.CardPlayed e ->
                    System.out.println(messages.get("event.card_played", cards.suitName(e.player()), cards.cardName(e.card())));
            case GameEvent.CardBeaten e ->
                    System.out.println(messages.get("event.card_beaten", cards.suitName(e.player()), cards.cardName(e.attacking()), cards.cardName(e.beating())));
            case GameEvent.PotTaken e ->
                    System.out.println(messages.get("event.pot_taken", cards.suitName(e.player()), cards.cardName(e.topCard()), e.potSize()));
            case GameEvent.RoundEnded e -> {
                System.out.println(messages.get("round.end"));
                System.out.println(messages.get("round.loser", cards.suitName(e.loser())));
                if (e.pushedToScoreboard() != null) {
                    System.out.println(messages.get("scoreboard.push",
                            cards.suitName(e.pushedToScoreboard().suit()), cards.cardName(e.pushedToScoreboard())));
                    printScoreboard(e.scoreboard());
                }
                if (e.eliminated()) {
                    System.out.println(messages.get("round.eliminated", cards.suitName(e.loser())));
                }
            }
            case GameEvent.GameEnded e -> {
                System.out.println(messages.get("game.ended"));
                System.out.println(messages.get("game.winner", cards.suitName(e.winner())));
            }
        }
    }

    private void printScoreboard(Map<Card.Suit, List<Card>> scoreboard) {
        System.out.println(messages.get("scoreboard.header"));
        for (Map.Entry<Card.Suit, List<Card>> entry : scoreboard.entrySet()) {
            System.out.println("  " + cards.suitName(entry.getKey()) + ": " + handText(entry.getValue()));
        }
    }

    private String handText(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cards.cardName(hand.get(i)));
        }
        return sb.toString();
    }
}
