package ru.anseranser.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The end of a round: the seat that still holds cards loses it, one of its
 * trumps goes onto its board, the rest of its cards and the pot come back to
 * the deck, and the next round is dealt from those cards.
 */
final class EndRound {

    private EndRound() {
    }

    /**
     * The state after the round ended: the loser's card is on its board, the
     * cards that came back are dealt to the seats still in the game and the
     * obligatory exchange is applied. The game is over when only one seat is
     * left in it, and then the cards that came back stay in the deck.
     *
     * <p>The transition reports the end of the round, the elimination of a
     * seat, the round that began and the cards of that round. The end of the
     * game is the last thing it reports.
     */
    static Transition apply(State state) {
        Transition resolution = resolve(state);
        List<Event> events = new ArrayList<>(resolution.events());
        if (Seats.countInGame(resolution.state().seats()) <= 1) {
            events.add(new Event.GameEnded(survivor(resolution.state())));
            return new Transition(resolution.state(), events);
        }
        Transition dealt = Dealing.dealNextRound(resolution.state());
        events.addAll(dealt.events());
        Transition exchanged = Exchange.apply(dealt.state());
        events.addAll(exchanged.events());
        return new Transition(exchanged.state(), events);
    }

    /**
     * The round resolved: the loser's card is on its board, the seat is
     * eliminated when its board is complete, the cards that came back are in
     * the deck and the dealer of the next round is in place.
     *
     * <p>The transition reports the end of the round and the elimination of the
     * seat whose board is complete.
     */
    static Transition resolve(State state) {
        List<Seat> seats = state.seats();
        int loserIndex = loserIndex(seats);
        Seat loser = seats.get(loserIndex);

        List<Card> pile = new ArrayList<>(loser.hand());
        pile.addAll(state.pot());

        Card pushed = cardForTheBoard(loser, pile);
        if (pushed != null) {
            pile.remove(pushed);
        }
        boolean eliminated = pushed == null || pushed.rank() == Card.Rank.ACE;

        List<Seat> resolved = new ArrayList<>(seats);
        resolved.set(loserIndex, new Seat(loser.trump(), List.of(),
                pushed == null ? loser.board() : boardWith(loser.board(), pushed), eliminated));

        Seat dealer = eliminated
                ? resolved.get(Seats.nextInGame(resolved, loserIndex))
                : resolved.get(loserIndex);
        State resolvedState = new State(resolved, List.of(), pile, dealer, dealer, false,
                state.turns(), state.rngState());

        List<Event> events = new ArrayList<>();
        events.add(new Event.RoundEnded(resolved.get(loserIndex),
                pushed == null ? Optional.empty() : Optional.of(pushed)));
        if (eliminated) {
            events.add(new Event.SeatEliminated(resolved.get(loserIndex)));
        }
        return new Transition(resolvedState, events);
    }

    /**
     * The loser: the seat that still holds cards. When several seats still hold
     * cards, as in a round stopped by the safety bound, the one with the most
     * cards loses and a tie is broken by the seating order.
     */
    private static int loserIndex(List<Seat> seats) {
        int loser = -1;
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            if (seat.out() || seat.hand().isEmpty()) {
                continue;
            }
            if (loser < 0 || seat.hand().size() > seats.get(loser).hand().size()) {
                loser = i;
            }
        }
        if (loser < 0) {
            throw new IllegalStateException("no seat holds cards, the round cannot end");
        }
        return loser;
    }

    /**
     * The card that goes onto the loser's board: the trump of the rank the
     * board requires next, or, when the pile holds no card of that rank, the
     * lowest trump of that suit in the pile.
     */
    private static Card cardForTheBoard(Seat loser, List<Card> pile) {
        Card.Rank required = Ladders.nextRequiredRank(loser.board());
        if (required != null) {
            for (Card card : pile) {
                if (card.suit() == loser.trump() && card.rank() == required) {
                    return card;
                }
            }
        }
        Card lowest = null;
        for (Card card : pile) {
            if (card.suit() != loser.trump()) {
                continue;
            }
            if (lowest == null || card.rank().ordinal() < lowest.rank().ordinal()) {
                lowest = card;
            }
        }
        return lowest;
    }

    private static List<Card> boardWith(List<Card> board, Card card) {
        List<Card> grown = new ArrayList<>(board);
        grown.add(card);
        return grown;
    }

    /** The seat that is still in the game when it ends. */
    private static Seat survivor(State state) {
        for (Seat seat : state.seats()) {
            if (!seat.out()) {
                return seat;
            }
        }
        throw new IllegalStateException("no seat is left in the game");
    }
}
