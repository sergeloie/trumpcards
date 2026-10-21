package ru.anseranser.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The trick: the cards that beat the top card of the pot, the moves the seat to
 * move may choose from, and the state that follows a move.
 *
 * <p>A turn of the old engine plays two cards, a beat and then a lead; a step
 * here plays one of them, and the flag that says the seat to move has to lead
 * the next trick survives the step that set it.
 */
final class Trick {

    /** The bound on the turns of one round. */
    private static final int MAX_ROUND_TURNS = 10_000;

    private Trick() {
    }

    /**
     * Whether {@code defending} beats {@code attacking} for a seat that plays
     * {@code trump}: the same suit with a higher rank, or the seat's own trump.
     */
    static boolean canBeat(Card.Suit trump, Card attacking, Card defending) {
        if (attacking.suit() == defending.suit()) {
            return attacking.rank().ordinal() < defending.rank().ordinal();
        }
        return defending.suit() == trump;
    }

    /**
     * The moves the seat to move may choose from: every card of its hand on a
     * fresh trick, the cards that beat the top card of the pot, or the pot
     * itself when no card beats it. The list is empty when the game is over.
     */
    static List<Move> legalMoves(State state) {
        if (Seats.countInGame(state.seats()) <= 1) {
            return List.of();
        }
        Seat seat = state.turn();
        if (state.leadRequired() || state.pot().isEmpty()) {
            return seat.hand().stream().<Move>map(Move.Play::new).toList();
        }
        Card top = state.pot().get(state.pot().size() - 1);
        List<Card> defences = validDefences(seat.trump(), top, seat.hand());
        if (defences.isEmpty()) {
            return List.of(new Move.TakePot());
        }
        return defences.stream().<Move>map(Move.Play::new).toList();
    }

    /**
     * The state after the move at {@code moveIndex}: the card played or the pot
     * taken, the turn advanced when the turn is over, and the round resolved
     * when it is over.
     *
     * <p>The transition reports the card that was played, whether it was a lead
     * or a beat, and the pot that was taken.
     */
    static Transition step(State state, int moveIndex) {
        Move move = legalMoves(state).get(moveIndex);

        List<Seat> seats = new ArrayList<>(state.seats());
        int turnIndex = seats.indexOf(state.turn());
        int dealerIndex = seats.indexOf(state.dealer());
        Seat seat = seats.get(turnIndex);
        boolean leading = state.leadRequired() || state.pot().isEmpty();
        Card top = state.pot().isEmpty() ? null : state.pot().get(state.pot().size() - 1);

        List<Card> hand = new ArrayList<>(seat.hand());
        List<Card> pot = new ArrayList<>(state.pot());
        if (move instanceof Move.Play play) {
            hand.remove(play.card());
            pot.add(play.card());
        } else {
            hand.addAll(pot);
            pot.clear();
        }
        Seat moved = new Seat(seat.trump(), hand, seat.board(), seat.out());
        seats.set(turnIndex, moved);

        List<Event> events = new ArrayList<>();
        if (move instanceof Move.Play play) {
            if (leading) {
                events.add(new Event.CardPlayed(moved, play.card()));
            } else {
                events.add(new Event.CardBeaten(moved, top, play.card()));
            }
        } else {
            events.add(new Event.PotTaken(moved, top, state.pot().size()));
        }

        boolean turnOver = !(move instanceof Move.Play) || leading || hand.isEmpty();
        int turns = state.turns();
        boolean leadRequired = false;
        Seat next = moved;
        if (turnOver) {
            turns++;
            next = seats.get(Seats.nextWithCards(seats, turnIndex));
        } else {
            leadRequired = true;
        }

        State advanced = new State(seats, pot, state.deck(), seats.get(dealerIndex), next,
                leadRequired, turns, state.rngState());
        if (!turnOver) {
            return new Transition(advanced, events);
        }
        if (Seats.countWithCards(seats) <= 1 || turns >= MAX_ROUND_TURNS) {
            Transition end = EndRound.apply(advanced);
            List<Event> all = new ArrayList<>(events);
            all.addAll(end.events());
            return new Transition(withTurnOnASeatThatCanAct(end).state(), all);
        }
        return new Transition(advanced, events);
    }

    /**
     * The cards of the hand that beat {@code attacking}, weakest first: the
     * cards that are not the trump of the seat come before the ones that are.
     */
    private static List<Card> validDefences(Card.Suit trump, Card attacking, List<Card> hand) {
        return hand.stream()
                .filter(card -> canBeat(trump, attacking, card))
                .sorted(Comparator.comparing((Card card) -> card.suit() == trump)
                        .thenComparing(card -> card.rank().ordinal()))
                .toList();
    }

    /**
     * The transition with the turn on a seat that can act: the dealer of the
     * next round may hold no card at all.
     */
    static Transition withTurnOnASeatThatCanAct(Transition transition) {
        State state = transition.state();
        if (Seats.countInGame(state.seats()) <= 1 || !state.turn().hand().isEmpty()) {
            return transition;
        }
        List<Seat> seats = state.seats();
        int turnIndex = seats.indexOf(state.turn());
        Seat next = seats.get(Seats.nextWithCards(seats, turnIndex));
        return new Transition(new State(seats, state.pot(), state.deck(), state.dealer(), next,
                false, state.turns(), state.rngState()), transition.events());
    }
}
