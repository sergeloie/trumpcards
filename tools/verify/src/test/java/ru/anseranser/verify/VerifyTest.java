package ru.anseranser.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.anseranser.bots.RuleBot;
import ru.anseranser.exporter.Exporter;
import ru.anseranser.exporter.Policy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The verifier: the games it plays, the invariants it asserts and the rounds
 * that hit the safety bound it reports.
 */
class VerifyTest {

    private static final Pattern WINNER = Pattern.compile("\"winner\":\"(\\w+)\"");

    @TempDir
    Path directory;

    @Test
    void theVerifierPlaysTheGamesOfTheSeedRange() {
        Run run = run(new String[]{"--seeds", "0:4", "--games", "2"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("verify: 2 games over the seeds 0:1"), run.out());
        assertTrue(run.out().contains("the invariants hold"), run.out());
    }

    @Test
    void theVerifierReportsTheRoundsThatHitTheSafetyBound() {
        Run run = run(new String[]{"--seeds", "12129:12129"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("1 of them hit the safety bound of 10000 turns"), run.out());
        assertTrue(run.out().contains("the seeds 12129"), run.out());
    }

    @Test
    void theVerifierReportsEverySeatThatWon() {
        Run run = run(new String[]{"--seeds", "0:3"});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("the winners: SPADES "), run.out());
        assertTrue(run.out().contains("CLUBS "), run.out());
        assertTrue(run.out().contains("DIAMONDS "), run.out());
        assertTrue(run.out().contains("HEARTS "), run.out());
    }

    @Test
    void theVerifierFailsLoudlyWhenAPolicyBreaksTheRules() {
        Policy breaking = (state, moves) -> moves.size();

        Run run = run(new String[]{"--seeds", "0:0"}, breaking, "a policy that breaks the rules");

        assertEquals(1, run.status(), "the tool must not say ok when a rule is broken");
        assertTrue(run.err().contains("seed 0"), run.err());
        assertTrue(run.err().contains("step "), run.err());
        assertTrue(run.err().contains("the policy chose the move"), run.err());
    }

    @Test
    void theVerifierChecksAnEpisodeFile() throws IOException {
        Path file = directory.resolve("episodes.jsonl");
        Exporter.main(new String[]{"--seeds", "0:2", "--out", file.toString()});

        Run run = run(new String[]{"--seeds", "0:0", "--episodes", file.toString()});

        assertEquals(0, run.status(), run.err());
        assertTrue(run.out().contains("3 episodes in "), run.out());
        assertTrue(run.out().contains("every one of them replays to the winner it was written with"), run.out());
    }

    @Test
    void theVerifierFailsLoudlyWhenAnEpisodeDoesNotReplayToItsWinner() throws IOException {
        Path file = directory.resolve("episodes.jsonl");
        Exporter.main(new String[]{"--seeds", "0:0", "--out", file.toString()});
        Files.writeString(file, tamper(Files.readString(file)));

        Run run = run(new String[]{"--seeds", "0:0", "--episodes", file.toString()});

        assertEquals(1, run.status(), "the tool must not say ok when an episode does not replay");
        assertTrue(run.err().contains("seed 0"), run.err());
        assertTrue(run.err().contains("replays to the winner"), run.err());
    }

    @Test
    void theVerifierFailsLoudlyWhenTheEpisodeFileIsNotThere() {
        Path missing = directory.resolve("missing.jsonl");

        Run run = run(new String[]{"--seeds", "0:0", "--episodes", missing.toString()});

        assertEquals(1, run.status(), "the tool must not say ok when the file cannot be read");
        assertTrue(run.err().contains("could not be read"), run.err());
    }

    @Test
    void theVerifierFailsLoudlyWhenTheEpisodeFileIsCorrupt() throws IOException {
        Path file = directory.resolve("corrupt.jsonl");
        Files.writeString(file, "this is not an episode");

        Run run = run(new String[]{"--seeds", "0:0", "--episodes", file.toString()});

        assertEquals(1, run.status(), "the tool must not say ok when the file cannot be read");
        assertTrue(run.err().contains("could not be read"), run.err());
    }

    @Test
    void theVerifierFailsOnAnOptionItDoesNotKnow() {
        assertThrows(IllegalArgumentException.class, () -> run(new String[]{"--nonsense", "1"}));
    }

    @Test
    void theVerifierFailsOnMoreGamesThanTheRangeHolds() {
        assertThrows(IllegalArgumentException.class,
                () -> run(new String[]{"--seeds", "0:1", "--games", "3"}));
    }

    /** The file, with the winner of every episode replaced by another suit. */
    private static String tamper(String file) {
        Matcher matcher = WINNER.matcher(file);
        StringBuilder tampered = new StringBuilder();
        while (matcher.find()) {
            String winner = matcher.group(1);
            String other = "SPADES".equals(winner) ? "HEARTS" : "SPADES";
            matcher.appendReplacement(tampered, Matcher.quoteReplacement("\"winner\":\"" + other + "\""));
        }
        matcher.appendTail(tampered);
        return tampered.toString();
    }

    private static Run run(String[] args) {
        return run(args, RuleBot::choose, "the rule bot");
    }

    private static Run run(String[] args, Policy policy, String name) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
        try {
            int status = Verify.run(args, policy, name);
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
