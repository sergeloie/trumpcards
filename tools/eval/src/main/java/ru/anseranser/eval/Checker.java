package ru.anseranser.eval;

import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Event;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;

import java.util.List;

/**
 * The checks of the invariants under a learned policy: every card of the deck
 * is in exactly one place at every step of the game, the game ends with
 * exactly one seat left in it, and neither the learned policy nor the rule bot
 * ever chooses a move that is not legal.
 *
 * <p>The cards of the deck are compared as a mask of bits, so that a card that
 * is lost and a card that is duplicated are both caught, and they are compared
 * at every step of the game and not only at its end: a card that goes missing
 * in the middle of a game and is put right again at the end would pass the end
 * of the game alone.
 */
final class Checker {

    /**
     * What one game left behind.
     *
     * @param seed the seed the game was played with
     * @param winner the trump suit of the seat that is left in the game
     * @param learned the trump suit of the seat the learned policy played
     * @param rounds the rounds the game took
     * @param cappedRounds the rounds that hit the safety bound
     * @param steps the steps the game took
     */
    record Result(long seed, Card.Suit winner, Card.Suit learned, int rounds, int cappedRounds, int steps) {
    }

    private Checker() {
    }

    /**
     * The game of the setup: the learned policy plays the seat of
     * {@code learnedSeat}, the rule bot plays the others, and the game is
     * checked at every step.
     *
     * <p>The round counter of the state counts the turns of the round that is
     * being played, and it begins again at the start of every round. A round
     * that hit the safety bound is therefore the round whose counter stood at
     * the bound when the round ended.
     */
    static Result play(Setup setup, Card.Suit learnedSeat, Policy learned) {
        int expected = setup.rankSet().cardCount(setup.trumpSuits().size());
        long ceiling = (long) Game.MAX_ROUND_TURNS * expected * setup.trumpSuits().size();
        Transition start = Game.start(setup);
        State state = start.state();
        check(state, expected, setup.seed(), 0);
        int rounds = rounds(start.events());
        int capped = 0;
        int turns = state.turns();
        int step = 0;
        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                break;
            }
            int index = choose(state, moves, learnedSeat, learned, setup.seed(), step);
            Transition transition = step(state, index, setup.seed(), step);
            state = transition.state();
            rounds += rounds(transition.events());
            step++;
            check(state, expected, setup.seed(), step);
            int next = state.turns();
            if (next < turns && turns + 1 >= Game.MAX_ROUND_TURNS) {
                capped++;
            }
            turns = next;
            if (step > ceiling) {
                throw new InvariantViolation(setup.seed(), step, "the game did not end");
            }
        }
        if (turns >= Game.MAX_ROUND_TURNS) {
            capped++;
        }
        return new Result(setup.seed(), winner(state, setup.seed(), step), learnedSeat,
                rounds, capped, step);
    }

    /**
     * The check of the cards in play: as many cards as the deck holds, and
     * every one of them a different card.
     */
    static void check(State state, int expected, long seed, int step) {
        int total = state.pot().size() + state.deck().size();
        long mask = 0;
        for (Seat seat : state.seats()) {
            total += seat.hand().size() + seat.board().size();
            for (Card card : seat.hand()) {
                mask |= bit(card);
            }
            for (Card card : seat.board()) {
                mask |= bit(card);
            }
        }
        for (Card card : state.pot()) {
            mask |= bit(card);
        }
        for (Card card : state.deck()) {
            mask |= bit(card);
        }
        int distinct = Long.bitCount(mask);
        if (total != expected || distinct != expected) {
            throw new InvariantViolation(seed, step, "the cards in play are " + total + ", "
                    + distinct + " of them different, expected " + expected);
        }
    }

    /** The seat that is left in the game when the game ends. */
    static Card.Suit winner(State state, long seed, int step) {
        List<Seat> left = state.seats().stream().filter(seat -> !seat.out()).toList();
        if (left.size() != 1) {
            throw new InvariantViolation(seed, step, "the game ended with " + left.size()
                    + " seats left in it, expected one");
        }
        return left.get(0).trump();
    }

    /** The move the seat to move takes: the learned policy at its seat, the rule bot at the others. */
    private static int choose(State state, List<Move> moves, Card.Suit learnedSeat, Policy learned,
            long seed, int step) {
        boolean isLearned = state.turn().trump() == learnedSeat;
        int index = isLearned ? learned.choose(state, moves) : RuleBot.choose(state, moves);
        if (index < 0 || index >= moves.size()) {
            throw new InvariantViolation(seed, step, "the "
                    + (isLearned ? "learned policy" : "rule bot") + " chose the move " + index
                    + ", and there are " + moves.size() + " legal moves");
        }
        return index;
    }

    /** The step of the game, with the failure of the rules named by the seed and the step. */
    private static Transition step(State state, int index, long seed, int step) {
        try {
            return Game.step(state, index);
        } catch (RuntimeException failure) {
            throw new InvariantViolation(seed, step, "the rules broke: " + failure.getMessage(), failure);
        }
    }

    /** The number of rounds that began in the events. */
    private static int rounds(List<Event> events) {
        int rounds = 0;
        for (Event event : events) {
            if (event instanceof Event.RoundStarted) {
                rounds++;
            }
        }
        return rounds;
    }

    /** The bit of the card in the mask of the cards in play. */
    private static long bit(Card card) {
        return 1L << (card.suit().ordinal() * Card.Rank.values().length + card.rank().ordinal());
    }
}
