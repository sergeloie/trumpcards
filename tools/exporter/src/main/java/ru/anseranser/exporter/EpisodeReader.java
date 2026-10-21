package ru.anseranser.exporter;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The reader of the episodes: the file, line by line, one game per line.
 */
public final class EpisodeReader implements Closeable {

    private final BufferedReader reader;
    private final Gson gson = new Gson();

    public EpisodeReader(Path file) throws IOException {
        this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
    }

    /** The next episode of the file, or nothing when the file is over. */
    public Optional<Episode> next() throws IOException {
        String line = reader.readLine();
        while (line != null && line.isBlank()) {
            line = reader.readLine();
        }
        if (line == null) {
            return Optional.empty();
        }
        return Optional.of(gson.fromJson(line, Episode.class));
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
