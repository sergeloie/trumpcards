package ru.anseranser.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Order of players around the table.
 *
 * Introduced in refactor Stage 3 to replace the bespoke
 * {@code CircularDoublyLinkedList}: a plain list with circular navigational
 * helpers. The ordering is fixed at construction time and never mutates, so the
 * type is trivially deterministic (no {@code ThreadLocalRandom}-based
 * "random anchor" that the old code used only to begin a full-circle walk).
 *
 * All "visit every player once" loops that previously relied on
 * {@code getRandom()} + {@code do/while(current != start)} are replaced by a
 * single deterministic pass, which also removes a hidden source of
 * non-determinism (useful for testing and for deterministic replay on UI ports).
 */
public class TurnOrder implements Iterable<Player> {

    private final List<Player> players;

    public TurnOrder(List<Player> players) {
        this.players = new ArrayList<>(players);
    }

    public int size() {
        return players.size();
    }

    public Player get(int index) {
        return players.get(index);
    }

    /** The player sitting to the left of {@code player} (next to act). */
    public Player next(Player player) {
        return players.get(nextIndex(indexOf(player)));
    }

    private int indexOf(Player player) {
        int idx = players.indexOf(player);
        if (idx < 0) {
            throw new NoSuchElementException("Player not in turn order: " + player);
        }
        return idx;
    }

    private int nextIndex(int i) {
        return (i + 1) % players.size();
    }

    /**
     * The next active player starting from {@code from} (exclusive), skipping
     * players that do not satisfy {@code active}. Assumes at least one player
     * satisfies the predicate (otherwise throws), which holds for the game's
     * "advance until someone can act" loops.
     */
    public Player nextActive(Player from, Predicate<Player> active) {
        Player current = from;
        do {
            current = next(current);
        } while (!active.test(current));
        return current;
    }

    /** Iterate over every player exactly once, in seating order. */
    @Override
    public Iterator<Player> iterator() {
        return players.iterator();
    }
}
