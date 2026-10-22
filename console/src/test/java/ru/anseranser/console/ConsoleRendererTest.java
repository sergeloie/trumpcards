package ru.anseranser.console;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Event;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;
import ru.anseranser.engine.Transition;
import ru.anseranser.i18n.Messages;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleRendererTest {

    private static final Card.Suit SPADES = Card.Suit.SPADES;
    private static final Card.Suit HEARTS = Card.Suit.HEARTS;
    private static final Card ACE = new Card(SPADES, Card.Rank.ACE);
    private static final Card SEVEN = new Card(SPADES, Card.Rank.SEVEN);
    private static final Card QUEEN = new Card(HEARTS, Card.Rank.QUEEN);
    private static final Card KING = new Card(HEARTS, Card.Rank.KING);

    @Test
    void rendersTheStartOfTheGame() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, Card.Suit.CLUBS, Card.Suit.DIAMONDS, HEARTS), 42L);
        String output = render(Game.start(setup));
        assertTrue(output.contains("=== THE GAME BEGINS ==="), output);
        assertTrue(output.contains("===== ROUND ====="), output);
        assertTrue(output.contains("Dealer: "), output);
        assertTrue(output.contains("Scoreboard:"), output);
        assertTrue(output.contains("Spades hand:"), output);
    }

    @Test
    void rendersTheCardPlayed() {
        String output = render(transitionOf(new Event.CardPlayed(seat(SPADES), ACE)));
        assertTrue(output.contains("Spades moves: Ace of Spades"), output);
    }

    @Test
    void rendersTheCardBeaten() {
        String output = render(transitionOf(new Event.CardBeaten(seat(HEARTS), QUEEN, KING)));
        assertTrue(output.contains("Hearts beat Queen of Hearts by card King of Hearts"), output);
    }

    @Test
    void rendersThePotTaken() {
        String output = render(transitionOf(new Event.PotTaken(seat(HEARTS), QUEEN, 3)));
        assertTrue(output.contains("can't beat Queen of Hearts -> take pot (3 cards)"), output);
    }

    @Test
    void rendersTheCardExchanged() {
        Event.CardsExchanged exchanged =
                new Event.CardsExchanged(seat(SPADES), seat(HEARTS), SEVEN);
        String output = render(transitionOf(exchanged));
        assertTrue(output.contains("Spades gives Seven of Spades to Hearts"), output);
    }

    @Test
    void rendersTheEndOfTheRound() {
        Event.RoundEnded ended = new Event.RoundEnded(seat(SPADES), Optional.of(SEVEN));
        String output = render(transitionOf(ended));
        assertTrue(output.contains("--- End of Round ---"), output);
        assertTrue(output.contains("Loser: Spades"), output);
        assertTrue(output.contains("To scoreboard Spades pushed: Seven of Spades"), output);
    }

    @Test
    void rendersTheEliminationOfTheSeat() {
        String output = render(transitionOf(new Event.SeatEliminated(seat(SPADES))));
        assertTrue(output.contains("Spades KICKED OUT from game"), output);
    }

    @Test
    void rendersTheWinner() {
        String output = render(transitionOf(new Event.GameEnded(seat(HEARTS))));
        assertTrue(output.contains("=== GAME ENDED ==="), output);
        assertTrue(output.contains("Winner is: Hearts"), output);
    }

    private Transition transitionOf(Event event) {
        Seat seat = seat(SPADES);
        State state = new State(List.of(seat), List.of(), List.of(), seat, seat, false, 0, 0L);
        return new Transition(state, List.of(event));
    }

    private Seat seat(Card.Suit trump) {
        return new Seat(trump, List.of(), List.of(), false);
    }

    private String render(Transition transition) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new ConsoleRenderer(out, new Messages(Locale.ENGLISH)).render(transition);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
