package ru.anseranser.engine;

import org.junit.jupiter.api.Test;
import ru.anseranser.bots.RuleBot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * The golden scenarios of the old engine, rewritten against the new API: the
 * obligatory exchange, the pot that cannot be beaten, the trick that hands the
 * lead back, the end of the round and the board.
 *
 * <p>The expectations are the ones of the old engine's scenarios. The layouts
 * are the new engine's own: the old scripted dealer handed the cards to the
 * seats directly, the new engine deals a deck, so a layout is a deck order.
 */
class GoldenScenarioTest {

    /** The seed whose first dealer is SPADES, as the fixed generator of the old scenarios. */
    private static final long SPADES_DEALER_SEED = 3L;

    /** The seed of the game the old engine wins with DIAMONDS. */
    private static final long WHOLE_GAME_SEED = 42L;

    /** The suits of the table, in the canonical order the deck is built in. */
    private static final List<Card.Suit> TRUMPS = List.of(SPADES, CLUBS, DIAMONDS, HEARTS);

    // ---------- Scenario 1: obligatory card exchange (rules.md §2.3) ----------

    @Test
    void obligatoryExchange_movesEverySeventhToItsSuitOwner() {
        Map<Card.Suit, List<Card>> layout = hands(Map.of(
                SPADES, List.of(card(CLUBS, SEVEN)),
                CLUBS, List.of(card(DIAMONDS, SEVEN)),
                DIAMONDS, List.of(card(HEARTS, SEVEN)),
                HEARTS, List.of(card(SPADES, SEVEN))), List.of(Card.Suit.values()));

        State state = Game.start(setup(layout, SPADES_DEALER_SEED)).state();

        for (Seat seat : state.seats()) {
            assertTrue(seat.hand().contains(new Card(seat.trump(), SEVEN)),
                    "Seat " + seat.trump() + " must hold the SEVEN of their own suit after the exchange");
            for (Card.Suit other : Card.Suit.values()) {
                if (other != seat.trump()) {
                    assertFalse(seat.hand().contains(new Card(other, SEVEN)),
                            "The SEVEN of " + other + " must have been handed to its owner");
                }
            }
        }
    }

    // ---------- Scenario 2: defender with no legal defense takes the pot (rules.md §3.4) ----------

    @Test
    void defenderWithNoLegalDefense_takesThePot() {
        Card clubsEight = card(CLUBS, EIGHT);

        List<Event> events = play(setup(cannotBeatLayout(), SPADES_DEALER_SEED), 2, clubsEight);

        List<Event.CardPlayed> played = ofType(events, Event.CardPlayed.class);
        List<Event.PotTaken> taken = ofType(events, Event.PotTaken.class);

        assertEquals(1, played.size());
        assertEquals(SPADES, played.get(0).seat().trump());
        assertEquals(clubsEight, played.get(0).card());

        assertEquals(1, taken.size());
        assertEquals(CLUBS, taken.get(0).seat().trump(), "The player to the leader's left must take the pot");
        assertEquals(clubsEight, taken.get(0).topCard());
        assertEquals(1, taken.get(0).potSize());
    }

    // ---------- Scenario 3: a successful beat hands the lead to the same player (rules.md §3.3) ----------

    @Test
    void successfulBeat_letsTheDefenderLeadTheNextTrick() {
        Card clubsEight = card(CLUBS, EIGHT);
        Card clubsNine = card(CLUBS, NINE);

        List<Event> events = play(setup(canBeatLayout(), SPADES_DEALER_SEED), 3, clubsEight);

        List<Event.CardBeaten> beaten = ofType(events, Event.CardBeaten.class);
        assertEquals(1, beaten.size());
        assertEquals(CLUBS, beaten.get(0).seat().trump());
        assertEquals(clubsEight, beaten.get(0).attacking());
        assertEquals(clubsNine, beaten.get(0).beating());

        List<Event.CardPlayed> played = ofType(events, Event.CardPlayed.class);
        assertEquals(2, played.size());
        assertEquals(CLUBS, played.get(1).seat().trump(),
                "The successful defender leads the next trick");
    }

    // ---------- Scenario 4: round end pushes the next trump (rules.md §6) ----------

    @Test
    void roundEnd_pushesNextTrumpToLoserBoard() {
        Map<Card.Suit, List<Card>> layout = hands(Map.of(), List.of(Card.Suit.values()));

        State state = Game.start(setup(layout, SPADES_DEALER_SEED)).state();
        List<Event> events = new ArrayList<>();
        while (ofType(events, Event.RoundEnded.class).isEmpty()) {
            List<Move> moves = Game.legalMoves(state);
            Transition transition = Game.step(state, RuleBot.choose(state, moves));
            events.addAll(transition.events());
            state = transition.state();
        }

        Event.RoundEnded ended = ofType(events, Event.RoundEnded.class).get(0);
        Card pushed = ended.pushedToBoard().orElse(null);
        assertNotNull(pushed, "First round: the loser still holds trumps, so a card is pushed");
        assertEquals(ended.loser().trump(), pushed.suit(), "The pushed card belongs to the loser's trump suit");
        assertEquals(SEVEN, pushed.rank(),
                "After the base " + SIX + " the ladder's next required card is SEVEN");
        assertFalse(eliminated(events, ended), "Pushing a SEVEN does not complete the ladder");
        assertTrue(board(state, ended.loser().trump()).contains(pushed),
                "The pushed card must appear on the loser's board ladder");
    }

    // ---------- Scenario 5: elimination when the ladder reaches ACE (rules.md §4, §6) ----------

    @Test
    void wholeGame_completesLadderToAce_andEndsWithOneWinner() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, TRUMPS, WHOLE_GAME_SEED);
        Transition start = Game.start(setup);
        List<Event> events = new ArrayList<>(start.events());
        State state = start.state();
        while (!Game.legalMoves(state).isEmpty()) {
            List<Move> moves = Game.legalMoves(state);
            Transition transition = Game.step(state, RuleBot.choose(state, moves));
            events.addAll(transition.events());
            state = transition.state();
        }

        List<Event.RoundEnded> eliminations = new ArrayList<>();
        for (Event.RoundEnded ended : ofType(events, Event.RoundEnded.class)) {
            if (eliminated(events, ended)) {
                eliminations.add(ended);
            }
        }
        assertFalse(eliminations.isEmpty(), "expected at least one ladder-completing (ACE) elimination");
        assertEquals(ACE, eliminations.get(0).pushedToBoard().orElseThrow().rank(),
                "Elimination happens exactly when an ACE is pushed");

        List<Event.GameEnded> gameEnded = ofType(events, Event.GameEnded.class);
        assertEquals(1, gameEnded.size(), "GameEnded must fire exactly once");
        Seat survivor = state.seats().stream().filter(seat -> !seat.out()).findFirst().orElseThrow();
        assertEquals(1, state.seats().stream().filter(seat -> !seat.out()).count(),
                "Exactly one seat remains in the game");
        assertEquals(survivor, gameEnded.get(0).winner(), "The GameEnded winner must be the surviving gamer");
        assertEquals(DIAMONDS, gameEnded.get(0).winner().trump(),
                "Golden winner for the game of this seed, the one the old engine gives");
    }

    /**
     * The hands of the seats: the cards pinned to them first, then the rest of
     * the deck shared out among the seats of the fill order.
     */
    private static Map<Card.Suit, List<Card>> hands(Map<Card.Suit, List<Card>> pinned, List<Card.Suit> fillOrder) {
        Map<Card.Suit, List<Card>> hands = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            hands.put(suit, new ArrayList<>(pinned.getOrDefault(suit, List.of())));
        }
        Set<Card> used = new HashSet<>();
        for (List<Card> cards : pinned.values()) {
            used.addAll(cards);
        }
        int dealt = 0;
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : RankSet.SIX_TO_ACE.ranks()) {
                if (rank == RankSet.SIX_TO_ACE.baseRank()) {
                    continue;
                }
                Card card = new Card(suit, rank);
                if (used.add(card)) {
                    hands.get(fillOrder.get(dealt++ % fillOrder.size())).add(card);
                }
            }
        }
        return hands;
    }

    /** The setup that deals the hands to the seats, the dealer of the seed included. */
    private static Setup setup(Map<Card.Suit, List<Card>> hands, long seed) {
        int dealer = new SplitMix64(seed).nextInt(TRUMPS.size());
        return new Setup(RankSet.SIX_TO_ACE, TRUMPS, seed, deckOrder(hands, TRUMPS, dealer));
    }

    /**
     * The deck order that deals the hands to the seats, one card at a time,
     * starting at the seat to the left of the dealer.
     */
    private static List<Card> deckOrder(Map<Card.Suit, List<Card>> hands, List<Card.Suit> trumps, int dealer) {
        int seats = trumps.size();
        int size = hands.get(trumps.get(0)).size();
        List<Card> deck = new ArrayList<>();
        for (int round = 0; round < size; round++) {
            for (int seat = 0; seat < seats; seat++) {
                deck.add(hands.get(trumps.get((dealer + 1 + seat) % seats)).get(round));
            }
        }
        return deck;
    }

    /**
     * The layout of the pot that cannot be beaten: the seat that leads holds
     * the eight of clubs, and the seat that follows holds nothing that beats
     * it.
     */
    private static Map<Card.Suit, List<Card>> cannotBeatLayout() {
        return Map.of(
                SPADES, List.of(card(CLUBS, EIGHT), card(SPADES, SEVEN), card(SPADES, EIGHT),
                        card(SPADES, NINE), card(SPADES, TEN), card(SPADES, JACK),
                        card(SPADES, QUEEN), card(SPADES, KING)),
                CLUBS, List.of(card(CLUBS, SEVEN), card(SPADES, ACE), card(DIAMONDS, EIGHT),
                        card(DIAMONDS, NINE), card(DIAMONDS, TEN), card(DIAMONDS, JACK),
                        card(DIAMONDS, QUEEN), card(DIAMONDS, KING)),
                DIAMONDS, List.of(card(CLUBS, NINE), card(CLUBS, TEN), card(CLUBS, JACK),
                        card(CLUBS, QUEEN), card(CLUBS, KING), card(CLUBS, ACE),
                        card(DIAMONDS, SEVEN), card(DIAMONDS, ACE)),
                HEARTS, List.of(card(HEARTS, SEVEN), card(HEARTS, EIGHT), card(HEARTS, NINE),
                        card(HEARTS, TEN), card(HEARTS, JACK), card(HEARTS, QUEEN),
                        card(HEARTS, KING), card(HEARTS, ACE)));
    }

    /**
     * The layout of the beat: the seat that follows the leader holds the nine
     * of clubs, and nothing that beats it more weakly.
     */
    private static Map<Card.Suit, List<Card>> canBeatLayout() {
        return Map.of(
                SPADES, List.of(card(CLUBS, EIGHT), card(SPADES, SEVEN), card(SPADES, EIGHT),
                        card(SPADES, NINE), card(SPADES, TEN), card(SPADES, JACK),
                        card(SPADES, QUEEN), card(SPADES, KING)),
                CLUBS, List.of(card(CLUBS, NINE), card(SPADES, ACE), card(DIAMONDS, EIGHT),
                        card(DIAMONDS, NINE), card(DIAMONDS, TEN), card(DIAMONDS, JACK),
                        card(DIAMONDS, QUEEN), card(DIAMONDS, KING)),
                DIAMONDS, List.of(card(CLUBS, SEVEN), card(CLUBS, TEN), card(CLUBS, JACK),
                        card(CLUBS, QUEEN), card(CLUBS, KING), card(CLUBS, ACE),
                        card(DIAMONDS, SEVEN), card(DIAMONDS, ACE)),
                HEARTS, List.of(card(HEARTS, SEVEN), card(HEARTS, EIGHT), card(HEARTS, NINE),
                        card(HEARTS, TEN), card(HEARTS, JACK), card(HEARTS, QUEEN),
                        card(HEARTS, KING), card(HEARTS, ACE)));
    }

    /** The events of the first steps of a game, with the pinned card opening the trick. */
    private static List<Event> play(Setup setup, int steps, Card pinnedLead) {
        State state = Game.start(setup).state();
        List<Event> events = new ArrayList<>();
        for (int step = 0; step < steps; step++) {
            List<Move> moves = Game.legalMoves(state);
            Transition transition = Game.step(state, choosePinnedLead(state, moves, pinnedLead));
            events.addAll(transition.events());
            state = transition.state();
        }
        return events;
    }

    /**
     * The move the scenario plays: the pinned card when the seat to move leads,
     * and the rule bot's move in every other case.
     */
    private static int choosePinnedLead(State state, List<Move> moves, Card pinned) {
        if (state.leadRequired() || state.pot().isEmpty()) {
            for (int i = 0; i < moves.size(); i++) {
                if (((Move.Play) moves.get(i)).card().equals(pinned)) {
                    return i;
                }
            }
        }
        return RuleBot.choose(state, moves);
    }

    /** Whether the seat that lost the round left the game with it. */
    private static boolean eliminated(List<Event> events, Event.RoundEnded ended) {
        int index = events.indexOf(ended);
        return index + 1 < events.size() && events.get(index + 1) instanceof Event.SeatEliminated;
    }

    private static List<Card> board(State state, Card.Suit trump) {
        for (Seat seat : state.seats()) {
            if (seat.trump() == trump) {
                return seat.board();
            }
        }
        throw new IllegalArgumentException("no seat plays " + trump);
    }

    private static <T extends Event> List<T> ofType(List<Event> events, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Event event : events) {
            if (type.isInstance(event)) {
                out.add(type.cast(event));
            }
        }
        return out;
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
