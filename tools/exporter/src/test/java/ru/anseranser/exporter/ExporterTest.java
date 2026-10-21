package ru.anseranser.exporter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The exporter: the games of a seed range, written to a file one line at a
 * time.
 */
class ExporterTest {

    @TempDir
    Path directory;

    @Test
    void theExporterWritesOneEpisodePerSeed() throws IOException {
        Path file = directory.resolve("episodes.jsonl");

        Exporter.main(new String[]{"--seeds", "3:5", "--out", file.toString()});

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size(), "one episode per seed");
        for (int i = 0; i < lines.size(); i++) {
            JsonObject json = JsonParser.parseString(lines.get(i)).getAsJsonObject();
            assertEquals(3L + i, json.getAsJsonObject("setup").get("seed").getAsLong(),
                    "the seeds come in order");
        }
    }

    @Test
    void theExporterWritesTheNumberOfGamesItIsAskedFor() throws IOException {
        Path file = directory.resolve("episodes.jsonl");

        Exporter.main(new String[]{"--seeds", "0:9", "--games", "2", "--out", file.toString()});

        assertEquals(2, Files.readAllLines(file).size(), "the exporter writes the games it is asked for");
    }

    @Test
    void theExporterFailsOnAModelThatIsNotThere() {
        Path file = directory.resolve("episodes.jsonl");
        Path missing = directory.resolve("missing.onnx");

        assertThrows(IllegalArgumentException.class, () -> Exporter.main(new String[]{
                "--seeds", "0:0", "--out", file.toString(), "--model", missing.toString()}));
    }

    @Test
    void theExporterFailsOnAnOptionItDoesNotKnow() {
        assertThrows(IllegalArgumentException.class,
                () -> Exporter.main(new String[]{"--nonsense", "1"}));
    }

    @Test
    void theExporterFailsOnMoreGamesThanTheRangeHolds() {
        Path file = directory.resolve("episodes.jsonl");

        assertThrows(IllegalArgumentException.class, () -> Exporter.main(new String[]{
                "--seeds", "0:1", "--games", "3", "--out", file.toString()}));
    }
}
