package ru.anseranser.exporter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.State;
import ru.anseranser.ml.Vector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The episode: the format of a training episode, the round trip through the
 * file and the replay of what the file holds.
 */
class EpisodeTest {

    @TempDir
    Path directory;

    @Test
    void theEpisodeHoldsTheSetupTheStepsAndTheWinner() throws IOException {
        Path file = directory.resolve("episode.jsonl");
        write(file, Episode.play(setup(42L), RuleBot::choose));

        JsonObject json = JsonParser.parseString(firstLine(file)).getAsJsonObject();

        assertEquals(List.of("setup", "steps", "winner"), names(json),
                "the episode holds the setup, the steps and the winner");

        JsonObject setup = json.getAsJsonObject("setup");
        assertEquals(List.of("rankSet", "trumpSuits", "seed"), names(setup),
                "the setup holds the rank set, the trump suits and the seed");
        assertEquals("SIX_TO_ACE", setup.get("rankSet").getAsString());
        assertEquals(List.of("SPADES", "CLUBS", "DIAMONDS", "HEARTS"), strings(setup.getAsJsonArray("trumpSuits")));
        assertEquals(42L, setup.get("seed").getAsLong());

        JsonArray steps = json.getAsJsonArray("steps");
        assertFalse(steps.isEmpty(), "the game was played");
        JsonObject step = steps.get(0).getAsJsonObject();
        assertEquals(List.of("observation", "mask", "action", "legalMoves"), names(step),
                "a step holds the observation, the mask, the action and the legal moves");
        assertEquals(Vector.LENGTH, step.getAsJsonArray("observation").size(),
                "the observation holds every value of the vector");

        assertTrue(List.of("SPADES", "CLUBS", "DIAMONDS", "HEARTS").contains(json.get("winner").getAsString()),
                "the episode names the seat that won");
    }

    @Test
    void theFileHoldsOneJsonObjectPerLine() throws IOException {
        Path file = directory.resolve("episodes.jsonl");
        try (EpisodeWriter writer = new EpisodeWriter(file)) {
            for (long seed = 0; seed < 3; seed++) {
                writer.write(Episode.play(setup(seed), RuleBot::choose));
            }
        }

        List<String> lines = Files.readAllLines(file);

        assertEquals(3, lines.size(), "one line per episode");
        for (String line : lines) {
            assertTrue(line.startsWith("{") && line.endsWith("}"), "the line holds one JSON object");
            assertNotNull(JsonParser.parseString(line).getAsJsonObject(), "the line is a JSON object");
        }
    }

    @Test
    void theEpisodeRoundTrips() throws IOException {
        Path file = directory.resolve("episode.jsonl");
        Episode episode = Episode.play(setup(7L), RuleBot::choose);
        write(file, episode);

        Episode read = read(file);

        assertEquals(episode.setup(), read.setup(), "the setup survives the file");
        assertEquals(episode.steps(), read.steps(), "the steps survive the file");
        assertEquals(episode.winner(), read.winner(), "the winner survives the file");
    }

    @Test
    void theEpisodeReplaysToTheSameOutcome() throws IOException {
        Path file = directory.resolve("episode.jsonl");
        write(file, Episode.play(setup(7L), RuleBot::choose));

        Episode read = read(file);
        State state = read.replay();

        assertEquals(read.winner(), Episode.winnerOf(state), "the episode ends with the seat it names");
    }

    @Test
    void everyStepHoldsTheObservationTheMaskAndTheLegalMoves() {
        Episode episode = Episode.play(setup(11L), RuleBot::choose);

        State state = Game.start(episode.setup().toEngineSetup()).state();
        int steps = 0;
        for (Episode.Step written : episode.steps()) {
            List<Move> moves = Game.legalMoves(state);
            Vector vector = Vector.of(state, state.turn(), moves);

            assertArrayEquals(vector.values(), written.observation(),
                    "the step holds the vector of the seat that moves");
            assertEquals(vector.mask(), written.mask(), "the step holds the mask of the legal moves");
            assertEquals(moves.size(), written.legalMoves().size(),
                    "the step holds every move that was on offer");
            assertEquals(maskOf(written.legalMoves()), written.mask(),
                    "the mask holds exactly the moves that were on offer");
            assertTrue(written.legalMoves().contains(written.action()),
                    "the action is among the moves that were on offer");

            state = Game.step(state, indexOf(written)).state();
            steps++;
        }

        assertTrue(steps > 0, "the game was played");
        assertEquals(episode.winner(), Episode.winnerOf(state), "the game ends with the winner the episode names");
    }

    @Test
    void theActionIsTheActionOfTheMoveAndNotItsIndex() {
        Episode episode = Episode.play(setup(13L), RuleBot::choose);

        State state = Game.start(episode.setup().toEngineSetup()).state();
        int different = 0;
        for (Episode.Step written : episode.steps()) {
            List<Move> moves = Game.legalMoves(state);
            Move move = moves.get(indexOf(written));
            int action = move instanceof Move.Play play ? Vector.actionOf(play.card()) : Vector.TAKE_POT;

            assertEquals(action, written.action(), "the step names the action of the move");
            if (action != indexOf(written)) {
                different++;
            }
            state = Game.step(state, indexOf(written)).state();
        }

        assertTrue(different > 0, "the action of a card is not the index of the move");
    }

    @Test
    void takingThePotIsTheActionFiftyTwo() {
        List<Episode> episodes = new ArrayList<>();
        for (long seed = 0; seed < 5; seed++) {
            episodes.add(Episode.play(setup(seed), RuleBot::choose));
        }

        boolean took = false;
        for (Episode episode : episodes) {
            for (Episode.Step written : episode.steps()) {
                if (written.action() == Vector.TAKE_POT) {
                    took = true;
                    assertEquals(List.of(Vector.TAKE_POT), written.legalMoves(),
                            "taking the pot is the only move on offer");
                }
            }
        }

        assertTrue(took, "a seat took the pot in one of the games");
    }

    private static Episode.Setup setup(long seed) {
        return new Episode.Setup(RankSet.SIX_TO_ACE, List.of(SPADES, CLUBS, DIAMONDS, HEARTS), seed);
    }

    /** The index of the move the step chose: the place of the action among the legal moves. */
    private static int indexOf(Episode.Step step) {
        return step.legalMoves().indexOf(step.action());
    }

    /** The mask the actions describe: a bit for every action that was on offer. */
    private static long maskOf(List<Integer> actions) {
        long mask = 0;
        for (int action : actions) {
            mask |= 1L << action;
        }
        return mask;
    }

    private static void write(Path file, Episode episode) throws IOException {
        try (EpisodeWriter writer = new EpisodeWriter(file)) {
            writer.write(episode);
        }
    }

    private static Episode read(Path file) throws IOException {
        try (EpisodeReader reader = new EpisodeReader(file)) {
            return reader.next().orElseThrow();
        }
    }

    private static String firstLine(Path file) throws IOException {
        return Files.readAllLines(file).get(0);
    }

    private static List<String> names(JsonObject json) {
        return List.copyOf(json.keySet());
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.getAsString()));
        return values;
    }
}
