/* Console platform entry point. Wires the console listener + input to the
 * :core engine and runs the game. This module is a thin composition root:
 * it contains no game rules, only platform glue (System.in/out). */
package ru.anseranser.console;

import ru.anseranser.event.ConsoleGameListener;
import ru.anseranser.input.ConsoleInputProvider;
import ru.anseranser.input.HumanDecisionStrategy;
import ru.anseranser.model.AiDecisionStrategy;
import ru.anseranser.model.Card;
import ru.anseranser.model.DecisionStrategy;
import ru.anseranser.model.DeckSize;
import ru.anseranser.model.Game;
import ru.anseranser.model.Player;

import java.util.ArrayList;
import java.util.List;

public class ConsoleLauncher {

    public static void main(String[] args) {
        // Composition root: wire the human seat (SPADES) to a console-driven
        // decision strategy and the rest to the AI. The domain (Game/Player)
        // knows nothing about consoles or input — it only sees DecisionStrategy.
        ConsoleInputProvider input = new ConsoleInputProvider();

        // Launch-time choice: play with a 36- or 52-card deck.
        DeckSize deckSize = input.chooseDeckSize();

        List<Player> players = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            DecisionStrategy strategy = (suit == Card.Suit.SPADES)
                    ? new HumanDecisionStrategy(input)
                    : new AiDecisionStrategy();
            players.add(new Player(suit, strategy));
        }

        Game game = new Game(players, deckSize);
        game.setListener(new ConsoleGameListener());
        game.playGame();
    }
}
