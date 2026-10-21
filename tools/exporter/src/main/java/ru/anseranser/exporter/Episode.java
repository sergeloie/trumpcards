package ru.anseranser.exporter;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.State;
import ru.anseranser.ml.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One game, written down so that a trainer in another language can learn from
 * it without knowing the rules.
 *
 * <p>The episode holds the setup of the game, every step of it and the seat
 * that won. A step holds the observation the policy saw, the mask over the
 * actions it may choose, the action it chose and the actions that were on
 * offer.
 *
 * <p>An action is the slot of a card in the action space — the suits in the
 * canonical order, the ranks ascending — or {@link Vector#TAKE_POT} for
 * taking the pot. The legal moves are the actions of the legal moves in the
 * order the engine lists them, so the place of an action in that list is the
 * index the engine wants.
 */
public record Episode(Setup setup, List<Step> steps, Card.Suit winner) {

    /** The setup of the game: the rank set, the trump suits and the seed. */
    public record Setup(RankSet rankSet, List<Card.Suit> trumpSuits, long seed) {

        public Setup {
            trumpSuits = List.copyOf(trumpSuits);
        }

        /** The setup the engine plays. */
        public ru.anseranser.engine.Setup toEngineSetup() {
            return new ru.anseranser.engine.Setup(rankSet, trumpSuits, seed);
        }
    }

    /** One step of the game: what the policy saw and what it chose. */
    public record Step(float[] observation, long mask, int action, List<Integer> legalMoves) {

        public Step {
            legalMoves = List.copyOf(legalMoves);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Step step
                    && mask == step.mask
                    && action == step.action
                    && legalMoves.equals(step.legalMoves)
                    && Arrays.equals(observation, step.observation);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * Long.hashCode(mask) + action) + legalMoves.hashCode())
                    + Arrays.hashCode(observation);
        }
    }

    public Episode {
        steps = List.copyOf(steps);
    }

    /** The game, played by the policy and written down. */
    public static Episode play(Setup setup, Policy policy) {
        State state = Game.start(setup.toEngineSetup()).state();
        List<Step> steps = new ArrayList<>();
        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                break;
            }
            int index = policy.choose(state, moves);
            Vector vector = Vector.of(state, state.turn(), moves);
            steps.add(new Step(vector.values(), vector.mask(), actionOf(moves.get(index)), actions(moves)));
            state = Game.step(state, index).state();
        }
        return new Episode(setup, steps, winnerOf(state));
    }

    /** The state the episode ends in: the setup played with the recorded actions. */
    public State replay() {
        State state = Game.start(setup.toEngineSetup()).state();
        for (Step step : steps) {
            state = Game.step(state, step.legalMoves().indexOf(step.action())).state();
        }
        return state;
    }

    /** The seat that wins the game the state ends. */
    public static Card.Suit winnerOf(State state) {
        List<Seat> left = state.seats().stream().filter(seat -> !seat.out()).toList();
        if (left.size() != 1) {
            throw new IllegalStateException("the game ended with " + left.size() + " seats left in it");
        }
        return left.get(0).trump();
    }

    /** The action a move plays. */
    private static int actionOf(Move move) {
        return move instanceof Move.Play play ? Vector.actionOf(play.card()) : Vector.TAKE_POT;
    }

    /** The actions of the legal moves, in the order the engine lists them. */
    private static List<Integer> actions(List<Move> moves) {
        return moves.stream().map(Episode::actionOf).toList();
    }
}
