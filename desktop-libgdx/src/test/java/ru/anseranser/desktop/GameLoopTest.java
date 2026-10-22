package ru.anseranser.desktop;

import org.junit.jupiter.api.Test;
import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Event;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.Transition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLoopTest {

    private static final List<Card.Suit> TRUMPS = List.of(
            Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS);

    @Test
    void playsAWholeGameToTheWinner() {
        List<Transition> transitions = new ArrayList<>();
        MoveChooser first = (state, moves) -> 0;

        new GameLoop(Card.Suit.SPADES, first, RuleBot::choose, transitions::add)
                .play(new Setup(RankSet.SIX_TO_ACE, TRUMPS, 42L));

        assertTrue(transitions.stream()
                        .flatMap(transition -> transition.events().stream())
                        .anyMatch(event -> event instanceof Event.GameEnded),
                "the game did not end");
    }

    @Test
    void theEventsOfTheGameAreTheOnesOfTheEngine() {
        List<Transition> transitions = new ArrayList<>();
        MoveChooser first = (state, moves) -> 0;

        new GameLoop(Card.Suit.SPADES, first, RuleBot::choose, transitions::add)
                .play(new Setup(RankSet.SIX_TO_ACE, TRUMPS, 42L));

        List<String> lines = new ArrayList<>();
        for (Transition transition : transitions) {
            transition.events().forEach(event -> lines.addAll(EventText.lines(event)));
        }
        assertTrue(lines.contains("Game started."), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("Round started. Dealer: ")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.contains(" takes the pot")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("Game over. Winner: ")), lines.toString());
    }
}
