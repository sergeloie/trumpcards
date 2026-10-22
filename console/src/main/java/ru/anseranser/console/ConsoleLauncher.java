package ru.anseranser.console;

import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * The text front end. It asks for the deck, plays the seat whose trump is
 * spades and lets the rule bot play the others.
 *
 * <p>The engine is the only source of truth: the console asks it for the
 * legal moves, hands it the index the player chose, and prints what the
 * transition reports.</p>
 */
public final class ConsoleLauncher {

    /** The seat the person plays. */
    static final Card.Suit HUMAN_SEAT = Card.Suit.SPADES;

    /** The suits in play, one per seat, in the seating order. */
    static final List<Card.Suit> TRUMPS = List.of(
            Card.Suit.SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS);

    private ConsoleLauncher() {
    }

    public static void main(String[] args) {
        play(new Scanner(System.in), System.out, System.nanoTime());
    }

    /** Plays one whole game, from the first deal to the winner. */
    static void play(Scanner in, PrintStream out, long seed) {
        ConsoleInput input = new ConsoleInput(in, out);
        ConsoleRenderer renderer = new ConsoleRenderer(out);

        RankSet rankSet = input.chooseRankSet();
        Setup setup = new Setup(rankSet, TRUMPS, seed);

        Transition transition = Game.start(setup);
        renderer.render(transition);
        State state = transition.state();

        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                return;
            }
            Seat seat = state.turn();
            int index = seat.trump() == HUMAN_SEAT
                    ? input.chooseMove(seat, moves)
                    : RuleBot.choose(state, moves);
            transition = Game.step(state, index);
            renderer.render(transition);
            state = transition.state();
        }
    }
}
