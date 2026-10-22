package ru.anseranser.console;

import org.junit.jupiter.api.Test;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleInputTest {

    private static final Card ACE = new Card(Card.Suit.SPADES, Card.Rank.ACE);
    private static final Card KING = new Card(Card.Suit.SPADES, Card.Rank.KING);
    private static final Seat SEAT = new Seat(Card.Suit.SPADES, List.of(), List.of(), false);

    @Test
    void choosesTheIndexOfTheCardThePlayerPicked() {
        Choice choice = chooseMove("2\n", List.of(new Move.Play(ACE), new Move.Play(KING)));
        assertEquals(1, choice.index());
        assertTrue(choice.output().contains("Spades - your turn"));
        assertTrue(choice.output().contains("Ace of Spades"));
        assertTrue(choice.output().contains("King of Spades"));
        assertTrue(choice.output().contains("Choose a move (1-2)"));
    }

    @Test
    void choosesTheIndexOfThePotToTake() {
        Choice choice = chooseMove("1\n", List.of(new Move.TakePot()));
        assertEquals(0, choice.index());
        assertTrue(choice.output().contains("take the pot"));
    }

    @Test
    void asksAgainWhenTheIndexIsOutOfRange() {
        Choice choice = chooseMove("7\n1\n", List.of(new Move.Play(ACE), new Move.Play(KING)));
        assertEquals(0, choice.index());
        assertTrue(choice.output().contains("Invalid input"));
    }

    @Test
    void asksAgainWhenTheAnswerIsNotANumber() {
        Choice choice = chooseMove("ace\n1\n", List.of(new Move.Play(ACE)));
        assertEquals(0, choice.index());
        assertTrue(choice.output().contains("Invalid input"));
    }

    @Test
    void choosesTheDeckToPlayWith() {
        assertEquals(RankSet.SIX_TO_ACE, chooseRankSet("1\n"));
        assertEquals(RankSet.TWO_TO_ACE, chooseRankSet("2\n"));
    }

    private Choice chooseMove(String answers, List<Move> moves) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        ConsoleInput input = new ConsoleInput(scanner(answers), out, new Messages(Locale.ENGLISH));
        int index = input.chooseMove(SEAT, moves);
        return new Choice(index, buffer.toString(StandardCharsets.UTF_8));
    }

    private RankSet chooseRankSet(String answers) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        ConsoleInput input = new ConsoleInput(scanner(answers), out, new Messages(Locale.ENGLISH));
        return input.chooseRankSet();
    }

    private Scanner scanner(String answers) {
        return new Scanner(new ByteArrayInputStream(answers.getBytes(StandardCharsets.UTF_8)));
    }

    private record Choice(int index, String output) {
    }
}
