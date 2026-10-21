package ru.anseranser.exporter;

import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.RankSet;
import ru.anseranser.ml.OnnxPolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * The exporter: writes the training episodes of a seed range as JSONL, one
 * game per line.
 *
 * <p>The games are played by the rule bot, or by an ONNX model given by path.
 * The seed range says which games to play, and every seed in it is one game.
 *
 * <pre>
 * exporter --seeds 0:99 --out episodes.jsonl
 * exporter --seeds 0:99 --out episodes.jsonl --model model.onnx
 * </pre>
 *
 * <p>The options are:
 *
 * <ul>
 *   <li>{@code --seeds from:to} — the seeds of the games; every seed in the
 *       range is one game;</li>
 *   <li>{@code --games count} — how many games to write, the whole range by
 *       default;</li>
 *   <li>{@code --out file} — the file to write, {@code episodes.jsonl} by
 *       default;</li>
 *   <li>{@code --rankset rankSet} — the rank set of the deck,
 *       {@code SIX_TO_ACE} by default;</li>
 *   <li>{@code --model file} — the ONNX model that plays the games, the rule
 *       bot by default.</li>
 * </ul>
 */
public final class Exporter {

    private static final List<Card.Suit> TRUMP_SUITS = List.of(Card.Suit.values());

    private Exporter() {
    }

    public static void main(String[] args) throws IOException {
        Settings settings = Settings.parse(args);
        if (settings.model() == null) {
            write(settings, RuleBot::choose);
            return;
        }
        try (OnnxPolicy model = new OnnxPolicy(settings.model())) {
            write(settings, model::choose);
        }
    }

    /** The games of the seed range, written to the file one line at a time. */
    private static void write(Settings settings, Policy policy) throws IOException {
        int written = 0;
        try (EpisodeWriter writer = new EpisodeWriter(settings.output())) {
            for (long seed = settings.from(); seed <= settings.to() && written < settings.games(); seed++) {
                Episode.Setup setup = new Episode.Setup(settings.rankSet(), TRUMP_SUITS, seed);
                writer.write(Episode.play(setup, policy));
                written++;
            }
        }
        System.out.println("wrote " + written + " episodes to " + settings.output());
    }

    /** The settings the exporter runs with. */
    private record Settings(long from, long to, int games, Path output, RankSet rankSet, Path model) {

        static Settings parse(String[] args) {
            long from = 0;
            long to = 99;
            int games = -1;
            Path output = Path.of("episodes.jsonl");
            RankSet rankSet = RankSet.SIX_TO_ACE;
            Path model = null;

            for (int i = 0; i < args.length; i += 2) {
                String option = args[i];
                if (i + 1 == args.length) {
                    throw new IllegalArgumentException("the option " + option + " needs a value");
                }
                String value = args[i + 1];
                switch (option) {
                    case "--seeds" -> {
                        String[] range = value.split(":");
                        if (range.length != 2) {
                            throw new IllegalArgumentException("the seed range is from:to, got " + value);
                        }
                        from = Long.parseLong(range[0]);
                        to = Long.parseLong(range[1]);
                    }
                    case "--games" -> games = Integer.parseInt(value);
                    case "--out" -> output = Path.of(value);
                    case "--rankset" -> rankSet = RankSet.valueOf(value);
                    case "--model" -> model = Path.of(value);
                    default -> throw new IllegalArgumentException("unknown option " + option);
                }
            }

            if (to < from) {
                throw new IllegalArgumentException("the seed range is empty: " + from + ":" + to);
            }
            long range = to - from + 1;
            if (games < 0) {
                games = (int) Math.min(range, Integer.MAX_VALUE);
            }
            if (games > range) {
                throw new IllegalArgumentException("the seed range holds " + range
                        + " seeds, " + games + " games were asked for");
            }
            return new Settings(from, to, games, output, rankSet, model);
        }
    }
}
