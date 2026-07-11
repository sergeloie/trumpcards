package ru.anseranser.event;

import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Console rendering of {@link GameEvent}s. This is the only place (in the
 * console target) that turns game data into human-readable text.
 *
 * Introduced in refactor Stage 1: it relocates all {@code System.out} that used
 * to live inside the model into the presentation layer, and centralizes the
 * user-facing strings so they can later be externalized for i18n.
 */
public class ConsoleGameListener implements GameListener {

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e -> System.out.println("=== THE GAME BEGINS ===");
            case GameEvent.RoundStarted e -> {
                System.out.println("\n===== ROUND =====");
                System.out.println("Dealer: " + e.dealer());
                printScoreboard(e.scoreboard());
                for (Map.Entry<Player, List<Card>> entry : e.hands().entrySet()) {
                    System.out.println(entry.getKey() + " hand: " + entry.getValue());
                }
            }
            case GameEvent.CardPlayed e ->
                    System.out.println("  " + e.player() + " moves: " + e.card());
            case GameEvent.CardBeaten e ->
                    System.out.println("  " + e.player() + " beat " + e.attacking() + " by card " + e.beating());
            case GameEvent.PotTaken e ->
                    System.out.println("  " + e.player() + " can't beat " + e.topCard() + " -> take pot (" + e.potSize() + " cards)");
            case GameEvent.RoundEnded e -> {
                System.out.println("\n--- End of Round ---");
                System.out.println("Loser: " + e.loser());
                if (e.pushedToScoreboard() != null) {
                    System.out.println("To scoreboard " + e.pushedToScoreboard().suit() + " pushed: " + e.pushedToScoreboard());
                    printScoreboard(e.scoreboard());
                }
                if (e.eliminated()) {
                    System.out.println(">>> " + e.loser() + " KICKED OUT from game (no more trumps) <<<");
                }
            }
            case GameEvent.GameEnded e -> {
                System.out.println("\n=== GAME ENDED ===");
                System.out.println("Winner is: " + e.winner());
            }
        }
    }

    private void printScoreboard(Map<Card.Suit, List<Card>> scoreboard) {
        System.out.println("Scoreboard:");
        for (Map.Entry<Card.Suit, List<Card>> entry : scoreboard.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
