package ru.anseranser.verify;

import ru.anseranser.bots.RuleBot;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Setup;
import ru.anseranser.exporter.Episode;
import ru.anseranser.exporter.EpisodeReader;
import ru.anseranser.exporter.Policy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The verifier: many games, played and checked, and the rounds that hit the
 * safety bound reported instead of hidden.
 *
 * <p>Every game is checked at every step: every card of the deck is in
 * exactly one place, the game ends with exactly one seat left in it, and the
 * policy never chooses a move that is not legal. A broken invariant stops the
 * tool, which then exits with a non-zero status and says which seed and which
 * step broke it.
 *
 * <pre>
 * verify --seeds 0:99
 * verify --seeds 0:99 --games 10 --rankset SIX_TO_ACE
 * verify --episodes episodes.jsonl
 * </pre>
 *
 * <p>The options are:
 *
 * <ul>
 *   <li>{@code --seeds from:to} — the seeds of the games; every seed in the
 *       range is one game;</li>
 *   <li>{@code --games count} — how many games to play, the whole range by
 *       default;</li>
 *   <li>{@code --rankset rankSet} — the rank set of the deck,
 *       {@code SIX_TO_ACE} by default;</li>
 *   <li>{@code --episodes file} — an episode file the exporter wrote, replayed
 *       and checked as well.</li>
 * </ul>
 */
public final class Verify {

    private static final List<Card.Suit> TRUMP_SUITS = List.of(Card.Suit.values());

    private Verify() {
    }

    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /** The games of the seed range, played by the rule bot and checked. */
    static int run(String[] args) {
        return run(args, RuleBot::choose, "the rule bot");
    }

    /** The games of the seed range, played by the policy and checked. */
    static int run(String[] args, Policy policy, String policyName) {
        Settings settings = Settings.parse(args);
        try {
            games(settings, policy, policyName);
        } catch (InvariantViolation violation) {
            System.err.println("verify: " + violation.getMessage());
            return 1;
        }
        if (settings.episodes() != null && !episodes(settings.episodes())) {
            return 1;
        }
        return 0;
    }

    /** The games of the seed range, played and checked, and what they left behind. */
    private static void games(Settings settings, Policy policy, String policyName) {
        int played = 0;
        int rounds = 0;
        int capped = 0;
        List<Long> cappedSeeds = new ArrayList<>();
        Map<Card.Suit, Integer> wins = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : TRUMP_SUITS) {
            wins.put(suit, 0);
        }

        for (long seed = settings.from(); seed <= settings.to() && played < settings.games(); seed++) {
            Checker.Result game = Checker.play(new Setup(settings.rankSet(), TRUMP_SUITS, seed), policy);
            played++;
            rounds += game.rounds();
            capped += game.cappedRounds();
            wins.merge(game.winner(), 1, Integer::sum);
            if (game.cappedRounds() > 0) {
                cappedSeeds.add(seed);
            }
        }

        String seeds = played == 0 ? "" : " over the seeds " + settings.from() + ":" + (settings.from() + played - 1);
        System.out.println("verify: " + played + " games" + seeds + ", rank set " + settings.rankSet()
                + ", policy: " + policyName);
        System.out.println("verify: " + rounds + " rounds, " + capped + " of them hit the safety bound of "
                + Game.MAX_ROUND_TURNS + " turns"
                + (cappedSeeds.isEmpty() ? "" : ", in the seeds " + join(cappedSeeds)));
        System.out.println("verify: the winners: " + join(wins));
        System.out.println("verify: the invariants hold");
    }

    /** The episodes of the file, replayed and checked. */
    private static boolean episodes(Path file) {
        int checked = 0;
        try (EpisodeReader reader = new EpisodeReader(file)) {
            Optional<Episode> next = reader.next();
            while (next.isPresent()) {
                Checker.check(next.get());
                checked++;
                next = reader.next();
            }
        } catch (InvariantViolation violation) {
            System.err.println("verify: " + violation.getMessage());
            return false;
        } catch (IOException | RuntimeException failure) {
            System.err.println("verify: the episodes could not be read: " + failure.getMessage());
            return false;
        }
        System.out.println("verify: " + checked + " episodes in " + file
                + ", every one of them replays to the winner it was written with");
        return true;
    }

    private static String join(List<Long> seeds) {
        return seeds.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String join(Map<Card.Suit, Integer> wins) {
        return wins.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /** The settings the verifier runs with. */
    private record Settings(long from, long to, int games, RankSet rankSet, Path episodes) {

        static Settings parse(String[] args) {
            long from = 0;
            long to = 99;
            int games = -1;
            RankSet rankSet = RankSet.SIX_TO_ACE;
            Path episodes = null;

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
                    case "--episodes" -> episodes = Path.of(value);
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
            return new Settings(from, to, games, rankSet, episodes);
        }
    }
}
