package ru.anseranser.engine;

import ru.anseranser.event.GameEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The old engine, driven the way the golden gate drives it: a game of four
 * seats, every one of them with the old {@code AiDecisionStrategy}, played with
 * a seeded generator.
 */
final class OldEngine {

    private OldEngine() {
    }

    /** The trace of the game the seed produces. */
    static List<String> trace(long seed) {
        ru.anseranser.model.Game game = new ru.anseranser.model.Game();
        List<GameEvent> events = new ArrayList<>();
        game.setListener(events::add);
        game.playGame(new ru.anseranser.model.SplitMix64(seed));
        return Trace.ofOld(events);
    }
}
