package ru.anseranser.desktop;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;

import java.util.List;
import java.util.function.Consumer;

/**
 * The loop of a game: the engine gives the legal moves, a policy chooses one
 * of them, and the engine reports what happened.
 *
 * <p>The person at the mouse chooses through the human {@link MoveChooser}; the
 * other seats are played by the bot. The rules live in the engine alone — this
 * loop only passes the index of the chosen move on.</p>
 */
final class GameLoop {

    private final Card.Suit humanSeat;
    private final MoveChooser human;
    private final MoveChooser bot;
    private final Consumer<Transition> onTransition;

    GameLoop(Card.Suit humanSeat, MoveChooser human, MoveChooser bot, Consumer<Transition> onTransition) {
        this.humanSeat = humanSeat;
        this.human = human;
        this.bot = bot;
        this.onTransition = onTransition;
    }

    /** Plays one whole game, from the first deal to the winner. */
    void play(Setup setup) {
        Transition transition = Game.start(setup);
        onTransition.accept(transition);
        State state = transition.state();

        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                return;
            }
            Seat seat = state.turn();
            int index = seat.trump() == humanSeat
                    ? human.choose(state, moves)
                    : bot.choose(state, moves);
            transition = Game.step(state, index);
            onTransition.accept(transition);
            state = transition.state();
        }
    }
}
