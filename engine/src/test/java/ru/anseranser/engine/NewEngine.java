package ru.anseranser.engine;

import ru.anseranser.bots.RuleBot;

import java.util.ArrayList;
import java.util.List;

import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The new engine, driven the way the golden gate drives it: the same seed, the
 * same setup and the rule bot.
 */
final class NewEngine {

    /** The suits of the table, in the canonical order the deck is built in. */
    static final List<Card.Suit> TRUMPS = List.of(SPADES, CLUBS, DIAMONDS, HEARTS);

    private NewEngine() {
    }

    /** The trace of the game the seed produces. */
    static List<String> trace(long seed) {
        return trace(new Setup(RankSet.SIX_TO_ACE, TRUMPS, seed));
    }

    /** The trace of a game of the setup, played by the rule bot. */
    static List<String> trace(Setup setup) {
        Transition start = Game.start(setup);
        List<Event> events = new ArrayList<>(start.events());
        State state = start.state();
        while (!Game.legalMoves(state).isEmpty()) {
            List<Move> moves = Game.legalMoves(state);
            Transition transition = Game.step(state, RuleBot.choose(state, moves));
            events.addAll(transition.events());
            state = transition.state();
        }
        return Trace.ofNew(events);
    }
}
