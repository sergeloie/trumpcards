package ru.anseranser.eval;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Setup;
import ru.anseranser.ml.OnnxPolicy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The evaluator: a learned policy against the rule bot over a seed range.
 *
 * <p>The learned policy takes one seat and the rule bot takes the others. The
 * seat of the learned policy moves from game to game, so that no seat of the
 * table is favoured over the others. The tool reports how often each seat won
 * and how often the learned policy won, and it asserts the same invariants the
 * verifier does while it plays: a policy that breaks the rules is caught even
 * when it is winning.
 *
 * <pre>
 * eval --model model.onnx --seeds 0:99
 * eval --model model.onnx --seeds 0:99 --games 10 --rankset SIX_TO_ACE
 * </pre>
 *
 * <p>The options are:
 *
 * <ul>
 *   <li>{@code --model file} — the ONNX model that plays, the learned
 *       policy; it is required;</li>
 *   <li>{@code --seeds from:to} — the seeds of the games; every seed in the
 *       range is one game;</li>
 *   <li>{@code --games count} — how many games to play, the whole range by
 *       default;</li>
 *   <li>{@code --rankset rankSet} — the rank set of the deck,
 *       {@code SIX_TO_ACE} by default.</li>
 * </ul>
 */
public final class Eval {

    private static final List<Card.Suit> TRUMP_SUITS = List.of(Card.Suit.values());

    private Eval() {
    }

    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /** The games of the seed range, played by the model against the rule bot. */
    static int run(String[] args) {
        Settings settings = Settings.parse(args);
        try (OnnxPolicy model = new OnnxPolicy(settings.model())) {
            Policy learned = model::choose;
            games(settings, learned);
            return 0;
        } catch (InvariantViolation violation) {
            System.err.println("eval: " + violation.getMessage());
            return 1;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            System.err.println("eval: the model could not be used: " + failure.getMessage());
            return 1;
        }
    }

    /** The games of the seed range, played and checked, and what they left behind. */
    private static void games(Settings settings, Policy learned) {
        int played = 0;
        int rounds = 0;
        int capped = 0;
        int learnedWins = 0;
        List<Long> cappedSeeds = new ArrayList<>();
        Map<Card.Suit, Integer> wins = new EnumMap<>(Card.Suit.class);
        Map<Card.Suit, Integer> seats = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : TRUMP_SUITS) {
            wins.put(suit, 0);
            seats.put(suit, 0);
        }

        for (long seed = settings.from(); seed <= settings.to() && played < settings.games(); seed++) {
            Card.Suit seat = TRUMP_SUITS.get(played % TRUMP_SUITS.size());
            Checker.Result game = Checker.play(new Setup(settings.rankSet(), TRUMP_SUITS, seed), seat, learned);
            played++;
            rounds += game.rounds();
            capped += game.cappedRounds();
            wins.merge(game.winner(), 1, Integer::sum);
            seats.merge(seat, 1, Integer::sum);
            if (game.winner() == seat) {
                learnedWins++;
            }
            if (game.cappedRounds() > 0) {
                cappedSeeds.add(seed);
            }
        }

        String seeds = played == 0 ? "" : " over the seeds " + settings.from() + ":" + (settings.from() + played - 1);
        System.out.println("eval: " + played + " games" + seeds + ", rank set " + settings.rankSet()
                + ", model " + settings.model());
        System.out.println("eval: the learned policy won " + learnedWins + " of " + played + " games ("
                + rate(learnedWins, played) + "), the rule bot won " + (played - learnedWins));
        System.out.println("eval: the wins: " + join(wins));
        System.out.println("eval: the games of the learned policy: " + join(seats));
        System.out.println("eval: " + rounds + " rounds, " + capped + " of them hit the safety bound of "
                + Game.MAX_ROUND_TURNS + " turns"
                + (cappedSeeds.isEmpty() ? "" : ", in the seeds " + join(cappedSeeds)));
        System.out.println("eval: the invariants hold");
    }

    private static String rate(int wins, int games) {
        if (games == 0) {
            return "0.0%";
        }
        return String.format(Locale.ROOT, "%.1f%%", 100.0 * wins / games);
    }

    private static String join(List<Long> seeds) {
        return seeds.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String join(Map<Card.Suit, Integer> counts) {
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /** The settings the evaluator runs with. */
    private record Settings(long from, long to, int games, RankSet rankSet, Path model) {

        static Settings parse(String[] args) {
            long from = 0;
            long to = 99;
            int games = -1;
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
                    case "--rankset" -> rankSet = RankSet.valueOf(value);
                    case "--model" -> model = Path.of(value);
                    default -> throw new IllegalArgumentException("unknown option " + option);
                }
            }

            if (model == null) {
                throw new IllegalArgumentException("the model of the learned policy is required");
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
            return new Settings(from, to, games, rankSet, model);
        }
    }
}
