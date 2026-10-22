package ru.anseranser.engine;

import ru.anseranser.bots.RuleBot;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives many full games of the new engine and reports what each of them left
 * behind: the cards in play, the surviving seat, the rounds the game took and
 * the rounds that hit the safety bound.
 *
 * <p>The seats are played by the rule bot of the {@code bots} module, the one
 * the golden gate drives the engine with, so the games are the games the old
 * engine plays.
 */
final class EngineSimulator {

    /**
     * What one game left behind.
     *
     * @param seed the seed the game was played with
     * @param winnerTrump the trump suit of the surviving seat
     * @param totalCards the cards in play when the game ended
     * @param distinctCards the different cards in play when the game ended
     * @param activeSeats the seats that are still in the game
     * @param rounds the rounds the game took
     * @param cappedRounds the rounds that hit the safety bound
     * @param expectedCards the cards the deck of the game holds
     */
    record Result(
            long seed,
            Card.Suit winnerTrump,
            int totalCards,
            int distinctCards,
            int activeSeats,
            int rounds,
            int cappedRounds,
            int expectedCards) {

        boolean invariantsHold() {
            return totalCards == expectedCards
                    && distinctCards == expectedCards
                    && activeSeats == 1
                    && winnerTrump != null;
        }
    }

    /** A game that was played: where it ended and what it took. */
    record Played(State state, int rounds, int cappedRounds) {
    }

    /** The cards in play: how many there are, and how many of them are different. */
    record Cards(int total, int distinct) {
    }

    private EngineSimulator() {
    }

    /** The results of {@code games} games, the seeds counting from {@code startSeed}. */
    static List<Result> run(RankSet rankSet, List<Card.Suit> trumps, long startSeed, int games) {
        List<Result> results = new ArrayList<>(games);
        for (int i = 0; i < games; i++) {
            results.add(result(new Setup(rankSet, trumps, startSeed + i)));
        }
        return results;
    }

    /** The result of the game the setup describes. */
    static Result result(Setup setup) {
        Played played = play(setup);
        State state = played.state();
        Cards cards = cardsInPlay(state);
        return new Result(
                setup.seed(),
                winner(state),
                cards.total(),
                cards.distinct(),
                Seats.countInGame(state.seats()),
                played.rounds(),
                played.cappedRounds(),
                setup.rankSet().cardCount(setup.trumpSuits().size()));
    }

    /** The state the game of the setup ends in. */
    static State finalState(Setup setup) {
        return play(setup).state();
    }

    /**
     * The game of the setup, played to its end.
     *
     * <p>The round counter of the state counts the turns of the round that is
     * being played, and it begins again at the start of every round. A round
     * that hit the safety bound is therefore the round whose counter stood at
     * the bound when the round ended.
     *
     * <p>The cards in play are checked at every step, not only at the end of
     * the game: a card that is lost or duplicated in the middle of a game and
     * put right again at the end would pass the end of the game alone.
     */
    private static Played play(Setup setup) {
        int expectedCards = setup.rankSet().cardCount(setup.trumpSuits().size());
        Transition start = Game.start(setup);
        State state = start.state();
        int rounds = roundsStarted(start.events());
        int cappedRounds = 0;
        int turns = state.turns();
        checkConservation(state, expectedCards, setup.seed(), 0);

        int step = 0;
        while (true) {
            List<Move> moves = Game.legalMoves(state);
            if (moves.isEmpty()) {
                break;
            }
            Transition transition = Game.step(state, RuleBot.choose(state, moves));
            state = transition.state();
            rounds += roundsStarted(transition.events());
            step++;
            checkConservation(state, expectedCards, setup.seed(), step);

            int next = state.turns();
            if (next < turns && turns + 1 >= Trick.MAX_ROUND_TURNS) {
                cappedRounds++;
            }
            turns = next;
        }

        if (turns >= Trick.MAX_ROUND_TURNS) {
            cappedRounds++;
        }
        return new Played(state, rounds, cappedRounds);
    }

    /**
     * The check of the cards in play: as many cards as the deck holds, and
     * every one of them a different card.
     */
    private static void checkConservation(State state, int expectedCards, long seed, int step) {
        Cards cards = cardsInPlay(state);
        if (cards.total() != expectedCards || cards.distinct() != expectedCards) {
            throw new IllegalStateException("seed " + seed + ", step " + step + ": the cards in play are "
                    + cards.total() + ", " + cards.distinct() + " of them different, expected " + expectedCards);
        }
    }

    /** The cards in play: the hands, the boards, the pot and the deck. */
    private static Cards cardsInPlay(State state) {
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
        return new Cards(total, Long.bitCount(mask));
    }

    /** The bit of the card in the mask of the cards in play. */
    private static long bit(Card card) {
        return 1L << (card.suit().ordinal() * Card.Rank.values().length + card.rank().ordinal());
    }

    /** The number of rounds that began in the events. */
    private static int roundsStarted(List<Event> events) {
        int rounds = 0;
        for (Event event : events) {
            if (event instanceof Event.RoundStarted) {
                rounds++;
            }
        }
        return rounds;
    }

    /** The trump suit of the seat that is still in the game. */
    private static Card.Suit winner(State state) {
        for (Seat seat : state.seats()) {
            if (!seat.out()) {
                return seat.trump();
            }
        }
        return null;
    }
}
