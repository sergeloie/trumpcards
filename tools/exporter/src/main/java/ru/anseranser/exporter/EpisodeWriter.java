package ru.anseranser.exporter;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The writer of the episodes: one game per line, and every line one JSON
 * object, so that a trainer can stream the file.
 */
public final class EpisodeWriter implements Closeable {

    private final BufferedWriter writer;
    private final Gson gson = new Gson();

    public EpisodeWriter(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
    }

    /** The episode, as one line of the file. */
    public void write(Episode episode) throws IOException {
        writer.write(gson.toJson(episode));
        writer.write('\n');
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
