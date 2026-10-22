package ru.anseranser.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Setup;
import ru.anseranser.ml.Vector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evaluator: the learned policy against the rule bot, the win rate it
 * reports and the invariants it asserts while it plays.
 */
class EvalTest {

    @TempDir
    Path directory;

    @Test
    void theEvaluatorPlaysTheGamesOfTheSeedRange() throws IOException {
        Path model = model();

        Run run = run(new String[]{"--model", model.toString(), "--seeds", "0:3", "--games", "2"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("eval: 2 games over the seeds 0:1"), run.out());
        assertTrue(run.out().contains("the learned policy won "), run.out());
        assertTrue(run.out().contains("the rule bot won "), run.out());
        assertTrue(run.out().contains("the invariants hold"), run.out());
    }

    @Test
    void theEvaluatorGivesTheLearnedPolicyEverySeat() throws IOException {
        Path model = model();

        Run run = run(new String[]{"--model", model.toString(), "--seeds", "0:3"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("the games of the learned policy: SPADES 1, CLUBS 1, DIAMONDS 1, HEARTS 1"),
                run.out());
    }

    @Test
    void theEvaluatorReportsTheWinsOfEverySeat() throws IOException {
        Path model = model();

        Run run = run(new String[]{"--model", model.toString(), "--seeds", "0:3"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("the wins: SPADES "), run.out());
        assertTrue(run.out().contains("CLUBS "), run.out());
        assertTrue(run.out().contains("DIAMONDS "), run.out());
        assertTrue(run.out().contains("HEARTS "), run.out());
    }

    @Test
    void theEvaluatorFailsLoudlyWhenTheModelIsNotThere() {
        Path missing = directory.resolve("missing.onnx");

        Run run = run(new String[]{"--model", missing.toString(), "--seeds", "0:0"});

        assertEquals(1, run.status(), "the tool must not say ok when the model cannot be used");
        assertTrue(run.err().contains("the model could not be used"), run.err());
    }

    @Test
    void theEvaluatorFailsWhenNoModelIsGiven() {
        assertThrows(IllegalArgumentException.class, () -> run(new String[]{"--seeds", "0:0"}));
    }

    @Test
    void theEvaluatorFailsOnAnOptionItDoesNotKnow() {
        assertThrows(IllegalArgumentException.class, () -> run(new String[]{"--nonsense", "1"}));
    }

    @Test
    void theCheckerCatchesAPolicyThatChoosesAnIllegalMove() {
        Policy breaking = (state, moves) -> moves.size();
        Setup setup = new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES, Card.Suit.CLUBS), 42L);

        InvariantViolation violation = assertThrows(InvariantViolation.class,
                () -> Checker.play(setup, Card.Suit.SPADES, breaking));

        assertTrue(violation.getMessage().contains("seed 42"), violation.getMessage());
        assertTrue(violation.getMessage().contains("the learned policy chose the move"), violation.getMessage());
    }

    @Test
    void theCheckerPlaysAGameAndReportsWhatItLeftBehind() {
        Setup setup = new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES, Card.Suit.CLUBS), 42L);

        Checker.Result game = Checker.play(setup, Card.Suit.SPADES, (state, moves) -> 0);

        assertEquals(42L, game.seed());
        assertEquals(Card.Suit.SPADES, game.learned());
        assertTrue(game.steps() > 0, "the game was played");
        assertTrue(game.rounds() >= 1, "the game played at least one round");
    }

    private Path model() throws IOException {
        return TinyModel.write(directory, Vector.LENGTH, Vector.ACTIONS);
    }

    private static Run run(String[] args) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
        try {
            int status = Eval.run(args);
            return new Run(status, outBuffer.toString(StandardCharsets.UTF_8),
                    errBuffer.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    /** What the tool did: the status it exits with and what it printed. */
    private record Run(int status, String out, String err) {
    }
}
