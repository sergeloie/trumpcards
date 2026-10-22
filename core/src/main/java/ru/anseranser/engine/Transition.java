package ru.anseranser.engine;

import java.util.List;

/**
 * The result of a call that produces a state: the new state and the things
 * that happened on the way. A start and a step return one, and so does every
 * rule of the engine, which is how the rules report what they did.
 */
public record Transition(State state, List<Event> events) {

    public Transition {
        events = List.copyOf(events);
    }
}
