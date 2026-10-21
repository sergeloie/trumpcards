package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.QUEEN;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * What every step reports: the card that was played, the round that ended,
 * the round that began and the game that ended. The events are handed to the
 * caller and are kept nowhere.
 */
class EventsTest {

    private static final List<Card.Suit> FOUR_SUITS = List.of(SPADES, CLUBS, DIAMONDS, HEARTS);

    @Test
    void theStartReportsTheGameTheRoundTheDealAndTheExchangeInOrder() {
        Transition transition = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L));

        assertEquals(List.of("GameStarted", "RoundStarted", "CardsDealt", "CardsDealt",
                "CardsDealt", "CardsDealt", "CardsExchanged", "CardsExchanged", "CardsExchanged"),
                kinds(transition.events()));
    }

    @Test
    void theStartReportsTheDealerOfTheRoundAndTheCardsEverySeatReceived() {
        Transition transition = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L));

        Event.RoundStarted started = (Event.RoundStarted) transition.events().get(1);
        assertEquals(DIAMONDS, started.dealer().trump());

        List<Event.CardsDealt> dealt = dealt(transition.events());
        assertEquals(List.of(SPADES, CLUBS, DIAMONDS, HEARTS),
                dealt.stream().map(event -> event.seat().trump()).toList());
        assertEquals(List.of(
                card(SPADES, EIGHT), card(CLUBS, SEVEN), card(CLUBS, QUEEN), card(CLUBS, TEN),
                card(DIAMONDS, SEVEN), card(CLUBS, ACE), card(HEARTS, JACK), card(HEARTS, ACE)),
                dealt.get(0).cards());
        assertEquals(List.of(
                card(SPADES, JACK), card(DIAMONDS, ACE), card(HEARTS, QUEEN), card(SPADES, NINE),
                card(HEARTS, EIGHT), card(SPADES, KING), card(DIAMONDS, TEN), card(DIAMONDS, JACK)),
                dealt.get(1).cards());
    }

    @Test
    void theStartReportsEveryCardTheObligatoryExchangePassed() {
        Transition transition = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L));

        List<Event.CardsExchanged> exchanged = exchanged(transition.events());
        assertEquals(3, exchanged.size());
        assertEquals(SPADES, exchanged.get(0).from().trump());
        assertEquals(CLUBS, exchanged.get(0).to().trump());
        assertEquals(card(CLUBS, SEVEN), exchanged.get(0).card());
        assertEquals(SPADES, exchanged.get(1).from().trump());
        assertEquals(DIAMONDS, exchanged.get(1).to().trump());
        assertEquals(card(DIAMONDS, SEVEN), exchanged.get(1).card());
        assertEquals(HEARTS, exchanged.get(2).from().trump());
        assertEquals(SPADES, exchanged.get(2).to().trump());
        assertEquals(card(SPADES, SEVEN), exchanged.get(2).card());
    }

    @Test
    void aLeadIsReportedAsACardPlayed() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));
        Seat diamonds = seat(DIAMONDS, List.of(card(DIAMONDS, NINE)));

        Transition transition = Game.step(
                state(List.of(spades, clubs, diamonds), List.of(), spades, false, 0), 0);

        assertEquals(List.of(new Event.CardPlayed(
                new Seat(SPADES, List.of(), List.of(card(SPADES, SIX)), false),
                card(SPADES, SEVEN))), transition.events());
    }

    @Test
    void aBeatIsReportedAsACardBeatenWithTheAttackingCard() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT), card(CLUBS, TEN)));

        Transition transition = Game.step(
                state(List.of(spades, clubs), List.of(card(SPADES, ACE)), clubs, false, 1), 0);

        assertEquals(List.of(new Event.CardBeaten(
                new Seat(CLUBS, List.of(card(CLUBS, TEN)), List.of(card(CLUBS, SIX)), false),
                card(SPADES, ACE), card(CLUBS, EIGHT))), transition.events());
    }

    @Test
    void takingThePotIsReportedWithTheTopCardAndTheSizeOfThePot() {
        Seat spades = seat(SPADES, List.of(card(CLUBS, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, EIGHT)));

        Transition transition = Game.step(
                state(List.of(spades, clubs), List.of(card(SPADES, ACE)), spades, false, 1), 0);

        assertEquals(List.of(new Event.PotTaken(
                new Seat(SPADES, List.of(card(CLUBS, SEVEN), card(SPADES, ACE)),
                        List.of(card(SPADES, SIX)), false),
                card(SPADES, ACE), 1)), transition.events());
    }

    @Test
    void aStepThatEndsTheRoundReportsTheRoundEndAndTheRoundThatFollowsIt() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN)));
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, SEVEN), card(CLUBS, NINE)));
        Seat diamonds = seat(DIAMONDS, List.of());
        Seat hearts = seat(HEARTS, List.of());

        Transition transition = Game.step(
                state(List.of(spades, clubs, diamonds, hearts), List.of(), spades, false, 0), 0);

        assertEquals(List.of(
                new Event.CardPlayed(
                        new Seat(SPADES, List.of(), List.of(card(SPADES, SIX)), false),
                        card(SPADES, SEVEN)),
                new Event.RoundEnded(
                        new Seat(CLUBS, List.of(), List.of(card(CLUBS, SIX), card(CLUBS, SEVEN)), false),
                        Optional.of(card(CLUBS, SEVEN))),
                new Event.RoundStarted(
                        new Seat(CLUBS, List.of(), List.of(card(CLUBS, SIX), card(CLUBS, SEVEN)), false)),
                new Event.CardsDealt(
                        new Seat(DIAMONDS, List.of(card(CLUBS, NINE)), List.of(card(DIAMONDS, SIX)), false),
                        List.of(card(CLUBS, NINE))),
                new Event.CardsDealt(
                        new Seat(HEARTS, List.of(card(SPADES, SEVEN)), List.of(card(HEARTS, SIX)), false),
                        List.of(card(SPADES, SEVEN))),
                new Event.CardsExchanged(
                        new Seat(HEARTS, List.of(), List.of(card(HEARTS, SIX)), false),
                        new Seat(SPADES, List.of(card(SPADES, SEVEN)), List.of(card(SPADES, SIX)), false),
                        card(SPADES, SEVEN))),
                transition.events());
    }

    @Test
    void aRoundThatBeginsWithOneSeatHoldingCardsEndsAtOnce() {
        Seat spades = seat(SPADES, List.of(card(SPADES, SEVEN), card(SPADES, ACE)));
        Seat hearts = seat(HEARTS, List.of());

        Transition transition = Game.step(state(List.of(spades, hearts), List.of(), spades, false, 0), 0);

        assertEquals(List.of(
                new Event.CardPlayed(
                        new Seat(SPADES, List.of(card(SPADES, ACE)), List.of(card(SPADES, SIX)), false),
                        card(SPADES, SEVEN)),
                new Event.RoundEnded(
                        new Seat(SPADES, List.of(), List.of(card(SPADES, SIX), card(SPADES, SEVEN)), false),
                        Optional.of(card(SPADES, SEVEN))),
                new Event.RoundStarted(
                        new Seat(SPADES, List.of(), List.of(card(SPADES, SIX), card(SPADES, SEVEN)), false)),
                new Event.CardsDealt(
                        new Seat(HEARTS, List.of(card(SPADES, ACE)), List.of(card(HEARTS, SIX)), false),
                        List.of(card(SPADES, ACE))),
                new Event.RoundEnded(
                        new Seat(HEARTS, List.of(), List.of(card(HEARTS, SIX)), true),
                        Optional.empty()),
                new Event.SeatEliminated(
                        new Seat(HEARTS, List.of(), List.of(card(HEARTS, SIX)), true)),
                new Event.GameEnded(
                        new Seat(SPADES, List.of(), List.of(card(SPADES, SIX), card(SPADES, SEVEN)), false))),
                transition.events());
    }

    @Test
    void aStepThatEndsTheGameReportsTheEliminationAndTheWinner() {
        Seat spades = new Seat(SPADES, List.of(card(SPADES, ACE)),
                board(SPADES, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING), false);
        Seat clubs = seat(CLUBS, List.of(card(CLUBS, SEVEN)));

        Transition transition = Game.step(
                state(List.of(spades, clubs), List.of(), clubs, false, 0), 0);

        Seat eliminated = new Seat(SPADES, List.of(),
                board(SPADES, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE), true);
        Seat winner = new Seat(CLUBS, List.of(), List.of(card(CLUBS, SIX)), false);
        assertEquals(List.of(
                new Event.CardPlayed(winner, card(CLUBS, SEVEN)),
                new Event.RoundEnded(eliminated, Optional.of(card(SPADES, ACE))),
                new Event.SeatEliminated(eliminated),
                new Event.GameEnded(winner)),
                transition.events());
    }

    @Test
    void theStateCarriesNoEvents() {
        assertEquals(List.of("seats", "pot", "deck", "dealer", "turn", "leadRequired", "turns",
                "rngState"), Arrays.stream(State.class.getRecordComponents())
                .map(RecordComponent::getName).toList());
    }

    @Test
    void aWholeGameIsReportedFromTheStartToTheEnd() {
        Transition start = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L));
        List<Event> events = new ArrayList<>(start.events());
        State state = start.state();

        int steps = 0;
        while (!Game.legalMoves(state).isEmpty()) {
            Transition step = Game.step(state, 0);
            events.addAll(step.events());
            state = step.state();
            steps++;
        }

        assertEquals(Event.GameStarted.class, events.get(0).getClass(),
                "the stream begins with the game that started");
        assertEquals(Event.GameEnded.class, events.get(events.size() - 1).getClass(),
                "the stream ends with the game that ended");

        long cardEvents = events.stream()
                .filter(event -> event instanceof Event.CardPlayed
                        || event instanceof Event.CardBeaten
                        || event instanceof Event.PotTaken)
                .count();
        assertEquals(steps, cardEvents, "every step reports the card it played");

        long rounds = events.stream().filter(event -> event instanceof Event.RoundStarted).count();
        assertTrue(rounds > 1, "the game lasts more than one round");

        List<Seat> inGame = state.seats().stream().filter(seat -> !seat.out()).toList();
        assertEquals(1, inGame.size(), "one seat is left in the game");
        Event.GameEnded ended = (Event.GameEnded) events.get(events.size() - 1);
        assertEquals(inGame.get(0).trump(), ended.winner().trump(), "the last seat standing wins");
    }

    private static List<String> kinds(List<Event> events) {
        return events.stream().map(event -> event.getClass().getSimpleName()).toList();
    }

    private static List<Event.CardsDealt> dealt(List<Event> events) {
        return events.stream().filter(Event.CardsDealt.class::isInstance)
                .map(Event.CardsDealt.class::cast).toList();
    }

    private static List<Event.CardsExchanged> exchanged(List<Event> events) {
        return events.stream().filter(Event.CardsExchanged.class::isInstance)
                .map(Event.CardsExchanged.class::cast).toList();
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }

    private static List<Card> board(Card.Suit suit, Card.Rank... ranks) {
        List<Card> cards = new ArrayList<>();
        for (Card.Rank rank : ranks) {
            cards.add(new Card(suit, rank));
        }
        return cards;
    }

    private static Seat seat(Card.Suit trump, List<Card> hand) {
        return new Seat(trump, hand, List.of(card(trump, SIX)), false);
    }

    private static State state(List<Seat> seats, List<Card> pot, Seat turn, boolean leadRequired,
            int turns) {
        return new State(seats, pot, List.of(), seats.get(0), turn, leadRequired, turns, 42L);
    }
}
