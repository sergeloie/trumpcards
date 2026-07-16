package ru.anseranser.model;

/**
 * Minimal random-number source used by the engine for shuffling.
 *
 * <p>The engine deliberately depends on this tiny interface rather than on
 * {@link java.util.Random} so that the RNG state can be captured and restored
 * for save/restore (R10) <em>without reflection</em>. {@code Random} hides its
 * internal seed behind a private field that module encapsulation blocks from
 * reflection on modern JDKs (and that is unavailable on Android / GraalVM
 * native image), so a custom, fully serializable generator is used instead.
 *
 * <p>Implementations must be deterministic: equal state in, equal sequence out.
 */
public interface Rng {

    /**
     * Returns a pseudo-random, uniformly distributed {@code int} in the half-open
     * range {@code [0, bound)}. Behaves like {@link java.util.Random#nextInt(int)}.
     *
     * @param bound the upper bound (exclusive); must be positive
     * @throws IllegalArgumentException if {@code bound <= 0}
     */
    int nextInt(int bound);

    /** The full internal state, as a single {@code long}, for serialization. */
    long getState();

    /** Restore the internal state from a previously captured {@link #getState()} value. */
    void setState(long state);
}
