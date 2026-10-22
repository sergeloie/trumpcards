package ru.anseranser.console;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleLauncherTest {

    @Test
    void playsAWholeGameToTheWinner() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        try {
            String output = play();
            assertTrue(output.contains("=== THE GAME BEGINS ==="), output);
            assertTrue(output.contains("===== ROUND ====="), output);
            assertTrue(output.contains("--- End of Round ---"), output);
            assertTrue(output.contains("=== GAME ENDED ==="), output);
            assertTrue(output.contains("Winner is: "), output);
        } finally {
            Locale.setDefault(previous);
        }
    }

    private String play() {
        String answers = "1\n" + "1\n".repeat(4000);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(new ByteArrayInputStream(answers.getBytes(StandardCharsets.UTF_8)));
        ConsoleLauncher.play(in, out, 42L);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
