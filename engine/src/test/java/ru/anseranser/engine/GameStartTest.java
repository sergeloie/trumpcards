package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.anseranser.engine.Card.Rank.ACE;
import static ru.anseranser.engine.Card.Rank.EIGHT;
import static ru.anseranser.engine.Card.Rank.FIVE;
import static ru.anseranser.engine.Card.Rank.JACK;
import static ru.anseranser.engine.Card.Rank.KING;
import static ru.anseranser.engine.Card.Rank.NINE;
import static ru.anseranser.engine.Card.Rank.QUEEN;
import static ru.anseranser.engine.Card.Rank.SEVEN;
import static ru.anseranser.engine.Card.Rank.SIX;
import static ru.anseranser.engine.Card.Rank.TEN;
import static ru.anseranser.engine.Card.Rank.TWO;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

class GameStartTest {

    private static final List<Card.Suit> FOUR_SUITS = List.of(SPADES, CLUBS, DIAMONDS, HEARTS);
    private static final List<Card.Suit> TWO_SUITS = List.of(HEARTS, SPADES);

    @Test
    void fourPlayersGetThirtySixCardsFourOnTheBoardsAndThirtyTwoDealt() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)).state();

        assertEquals(36, RankSet.SIX_TO_ACE.cardCount(4));
        assertEquals(36, cardsInPlay(state));
        assertEquals(4, cardsOnBoards(state));
        assertEquals(32, cardsInHands(state));
        assertEquals(List.of(8, 8, 8, 8), state.seats().stream().map(seat -> seat.hand().size()).toList());
    }

    @Test
    void twoPlayersGetEighteenCardsTwoOnTheBoardsAndSixteenDealt() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, TWO_SUITS, 42L)).state();

        assertEquals(18, RankSet.SIX_TO_ACE.cardCount(2));
        assertEquals(18, cardsInPlay(state));
        assertEquals(2, cardsOnBoards(state));
        assertEquals(16, cardsInHands(state));
        assertEquals(List.of(8, 8), state.seats().stream().map(seat -> seat.hand().size()).toList());
    }

    @Test
    void theFiftyTwoCardRankSetGivesFourPlayersFiftyTwoCardsAndFortyEightDealt() {
        State state = Game.start(new Setup(RankSet.TWO_TO_ACE, FOUR_SUITS, 42L)).state();

        assertEquals(52, RankSet.TWO_TO_ACE.cardCount(4));
        assertEquals(52, cardsInPlay(state));
        assertEquals(4, cardsOnBoards(state));
        assertEquals(48, cardsInHands(state));
        for (Seat seat : state.seats()) {
            assertEquals(List.of(new Card(seat.trump(), TWO)), seat.board());
        }
    }

    @Test
    void theDeckIsBuiltInTheCanonicalSuitOrderWhateverOrderTheSeatsAreListedIn() {
        Setup canonical = new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L);
        Setup shuffled = new Setup(RankSet.SIX_TO_ACE, List.of(HEARTS, DIAMONDS, CLUBS, SPADES), 42L);

        assertEquals(Dealing.deck(canonical), Dealing.deck(shuffled));

        List<Card> deck = Dealing.deck(canonical);
        assertEquals(32, deck.size());
        assertEquals(List.of(SPADES, CLUBS, DIAMONDS, HEARTS),
                deck.stream().map(Card::suit).distinct().toList());
        for (Card.Suit suit : List.of(SPADES, CLUBS, DIAMONDS, HEARTS)) {
            assertEquals(List.of(SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE),
                    deck.stream().filter(card -> card.suit() == suit).map(Card::rank).toList(),
                    "the ranks of " + suit + " must ascend");
        }
        assertEquals(List.of(), deck.stream().filter(card -> card.rank() == SIX).toList(),
                "the base card of every suit belongs on a board");
    }

    @Test
    void theBaseCardOfEverySuitIsOnTheBoardOfTheSeatThatOwnsIt() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)).state();

        for (Seat seat : state.seats()) {
            assertEquals(List.of(new Card(seat.trump(), SIX)), seat.board());
        }
    }

    @Test
    void theDealerIsChosenBeforeTheDeckIsShuffled() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)).state();

        assertEquals(DIAMONDS, state.dealer().trump());
        assertEquals(state.dealer(), state.turn(), "the dealer leads the first round");
        assertEquals(-4112007255848680758L, state.rngState());
    }

    @Test
    void theDealOfFourPlayersIsTheOneTheOldEngineDealsForTheSameSeed() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)).state();

        assertEquals(List.of(
                card(SPADES, EIGHT), card(CLUBS, SEVEN), card(CLUBS, QUEEN), card(CLUBS, TEN),
                card(DIAMONDS, SEVEN), card(CLUBS, ACE), card(HEARTS, JACK), card(HEARTS, ACE)),
                state.seats().get(0).hand());
        assertEquals(List.of(
                card(SPADES, JACK), card(DIAMONDS, ACE), card(HEARTS, QUEEN), card(SPADES, NINE),
                card(HEARTS, EIGHT), card(SPADES, KING), card(DIAMONDS, TEN), card(DIAMONDS, JACK)),
                state.seats().get(1).hand());
        assertEquals(List.of(
                card(CLUBS, NINE), card(CLUBS, EIGHT), card(CLUBS, KING), card(SPADES, ACE),
                card(HEARTS, TEN), card(CLUBS, JACK), card(DIAMONDS, EIGHT), card(SPADES, QUEEN)),
                state.seats().get(2).hand());
        assertEquals(List.of(
                card(HEARTS, NINE), card(DIAMONDS, NINE), card(DIAMONDS, QUEEN), card(SPADES, SEVEN),
                card(HEARTS, SEVEN), card(HEARTS, KING), card(DIAMONDS, KING), card(SPADES, TEN)),
                state.seats().get(3).hand());
    }

    @Test
    void theDealOfTwoPlayersIsTheOneTheOldEngineDealsForTheSameSeed() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, TWO_SUITS, 42L)).state();

        assertEquals(SPADES, state.dealer().trump());
        assertEquals(List.of(
                card(HEARTS, JACK), card(HEARTS, TEN), card(HEARTS, NINE), card(SPADES, EIGHT),
                card(HEARTS, ACE), card(SPADES, ACE), card(HEARTS, KING), card(SPADES, KING)),
                state.seats().get(0).hand());
        assertEquals(List.of(
                card(SPADES, SEVEN), card(HEARTS, SEVEN), card(HEARTS, EIGHT), card(SPADES, JACK),
                card(SPADES, TEN), card(HEARTS, QUEEN), card(SPADES, QUEEN), card(SPADES, NINE)),
                state.seats().get(1).hand());
        assertEquals(-2056003627924340358L, state.rngState());
    }

    @Test
    void theDeckIsDealtOneCardAtATimeFromTheSeatToTheLeftOfTheDealer() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L);
        List<Card> deck = Dealing.deck(setup);
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L, deck)).state();

        int firstSeat = (state.seats().indexOf(state.dealer()) + 1) % FOUR_SUITS.size();
        for (int i = 0; i < deck.size(); i++) {
            Seat seat = state.seats().get((firstSeat + i) % FOUR_SUITS.size());
            assertEquals(deck.get(i), seat.hand().get(i / FOUR_SUITS.size()), "card " + i);
        }
    }

    @Test
    void anExplicitDeckOrderIsDealtAsGivenAndIsNotShuffled() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L);
        List<Card> deck = Dealing.deck(setup);
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L, deck)).state();

        assertEquals(List.of(deck.get(0), deck.get(4), deck.get(8), deck.get(12),
                        deck.get(16), deck.get(20), deck.get(24), deck.get(28)),
                state.seats().get(3).hand());
        assertEquals(-7046029254386353089L, state.rngState(),
                "only the dealer is drawn when the deck order is given");
    }

    @Test
    void theSetupRejectsADeckOrderThatDoesNotFitTheGame() {
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L,
                        List.of(card(SPADES, SEVEN), card(SPADES, SEVEN))));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L, List.of(card(SPADES, SEVEN))));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L, deckWithADuplicate()));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L,
                        deckWith(card(SPADES, SIX))));
        assertThrows(IllegalArgumentException.class,
                () -> new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L,
                        deckWith(card(CLUBS, FIVE))));
    }

    private static List<Card> deckWith(Card card) {
        List<Card> deck = new ArrayList<>(Dealing.deck(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)));
        deck.set(0, card);
        return deck;
    }

    private static List<Card> deckWithADuplicate() {
        List<Card> deck = new ArrayList<>(Dealing.deck(new Setup(RankSet.SIX_TO_ACE, FOUR_SUITS, 42L)));
        deck.set(1, deck.get(0));
        return deck;
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }

    private static int cardsInPlay(State state) {
        return cardsInHands(state) + cardsOnBoards(state) + state.pot().size() + state.deck().size();
    }

    private static int cardsInHands(State state) {
        return state.seats().stream().mapToInt(seat -> seat.hand().size()).sum();
    }

    private static int cardsOnBoards(State state) {
        return state.seats().stream().mapToInt(seat -> seat.board().size()).sum();
    }
}
