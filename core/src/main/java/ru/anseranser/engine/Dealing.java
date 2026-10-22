package ru.anseranser.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The dealing rule: the deck is built from the suits in play, the base card of
 * every suit goes on the board of the seat that owns the suit, the rest is
 * shuffled and dealt one card at a time from the seat to the left of the
 * dealer.
 *
 * <p>The next round is dealt from the cards that came back from the deck pool,
 * not from a new deck.
 */
final class Dealing {

    private Dealing() {
    }

    /**
     * The first state of a game: the seats with their boards and their hands,
     * the dealer, and the position of the generator.
     *
     * <p>The draws are taken in the old engine's order: the dealer is chosen
     * first and the deck is shuffled afterwards.
     *
     * <p>The transition reports the round that began and the cards every seat
     * received.
     */
    static Transition deal(Setup setup) {
        List<Card.Suit> trumps = setup.trumpSuits();
        SplitMix64 rng = new SplitMix64(setup.seed());
        int dealerIndex = rng.nextInt(trumps.size());

        List<Card> deck;
        if (setup.deckOrder().isEmpty()) {
            deck = deck(setup);
            shuffle(deck, rng);
        } else {
            deck = new ArrayList<>(setup.deckOrder());
        }

        List<List<Card>> hands = hands(deck, trumps.size(), dealerIndex);
        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < trumps.size(); i++) {
            Card.Suit trump = trumps.get(i);
            seats.add(new Seat(trump, hands.get(i),
                    List.of(new Card(trump, setup.rankSet().baseRank())), false));
        }
        Seat dealer = seats.get(dealerIndex);
        State state = new State(seats, List.of(), List.of(), dealer, dealer, false, 0, rng.state());

        List<Event> events = new ArrayList<>();
        events.add(new Event.RoundStarted(dealer));
        for (Seat seat : seats) {
            events.add(new Event.CardsDealt(seat, seat.hand()));
        }
        return new Transition(state, events);
    }

    /**
     * The next round: the cards that came back from the deck pool are shuffled
     * and dealt to the seats still in the game, one card at a time, starting
     * from the seat to the left of the dealer.
     *
     * <p>The transition reports the round that began and the cards every seat
     * received; the cards a seat kept are not part of the deal.
     */
    static Transition dealNextRound(State state) {
        List<Card> pool = new ArrayList<>(state.deck());
        SplitMix64 rng = new SplitMix64(state.rngState());
        shuffle(pool, rng);

        List<Seat> seats = state.seats();
        int dealerIndex = Seats.indexOfTrump(seats, state.dealer().trump());
        List<List<Card>> dealt = dealHands(pool, seats, dealerIndex);

        List<Seat> newSeats = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            List<Card> hand = new ArrayList<>(seat.hand());
            hand.addAll(dealt.get(i));
            newSeats.add(new Seat(seat.trump(), hand, seat.board(), seat.out()));
        }
        State dealtState = new State(newSeats, state.pot(), List.of(),
                newSeats.get(dealerIndex),
                newSeats.get(Seats.indexOfTrump(seats, state.turn().trump())),
                false, 0, rng.state());

        List<Event> events = new ArrayList<>();
        events.add(new Event.RoundStarted(dealtState.dealer()));
        for (int i = 0; i < newSeats.size(); i++) {
            if (!dealt.get(i).isEmpty()) {
                events.add(new Event.CardsDealt(newSeats.get(i), dealt.get(i)));
            }
        }
        return new Transition(dealtState, events);
    }

    /**
     * The cards to deal, in the canonical order: by suit, and within a suit by
     * rank ascending. The base card of every suit is left out, because it goes
     * on the board of the seat that owns the suit.
     */
    static List<Card> deck(Setup setup) {
        List<Card> deck = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            if (!setup.trumpSuits().contains(suit)) {
                continue;
            }
            for (Card.Rank rank : setup.rankSet().ranks()) {
                if (rank != setup.rankSet().baseRank()) {
                    deck.add(new Card(suit, rank));
                }
            }
        }
        return deck;
    }

    /** The hands the deck is dealt into, one card at a time, in seating order. */
    private static List<List<Card>> hands(List<Card> deck, int seatCount, int dealerIndex) {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < seatCount; i++) {
            hands.add(new ArrayList<>());
        }
        int seat = (dealerIndex + 1) % seatCount;
        for (Card card : deck) {
            hands.get(seat).add(card);
            seat = (seat + 1) % seatCount;
        }
        return hands;
    }

    /**
     * The cards of the pool that every seat receives: the pool is dealt on top
     * of the hands that are left, and the seats that are out of the game
     * receive nothing.
     */
    private static List<List<Card>> dealHands(List<Card> pool, List<Seat> seats, int dealerIndex) {
        List<List<Card>> dealt = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            dealt.add(new ArrayList<>());
        }
        int seat = Seats.nextInGame(seats, dealerIndex);
        for (Card card : pool) {
            dealt.get(seat).add(card);
            seat = Seats.nextInGame(seats, seat);
        }
        return dealt;
    }

    /** Fisher-Yates from the end of the deck, as the old engine's dealer did. */
    private static void shuffle(List<Card> deck, SplitMix64 rng) {
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }
}
