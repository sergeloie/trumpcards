package ru.anseranser.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic, serializable pseudo-random generator (SplitMix64).
 *
 * <p>Chosen over {@link java.util.Random} because its entire state is a single
 * {@code long}, so it can be captured via {@link #getState()} and restored via
 * {@link #setState(long)} with no reflection. This keeps save/restore (R10)
 * portable to every target platform (desktop, web, mobile, GraalVM native
 * image) where reaching into {@code Random}'s private {@code seed} field is
 * impossible.
 *
 * <p>The output sequence is the standard SplitMix64 stream; it is uniformly
 * distributed and good enough for shuffling a 36-card deck. Two instances with
 * the same state produce the same sequence, which is what determinism tests and
 * game replay rely on.
 */
public final class SplitMix64 implements Rng {

    private static final AtomicLong SEED_COUNTER = new AtomicLong(System.nanoTime());

    private long state;

    /** Default seed; varies per instance so two un-seeded games differ. */
    public SplitMix64() {
        this(SEED_COUNTER.addAndGet(0x9E37_79B9_7F4A_7C15L));
    }

    public SplitMix64(long seed) {
        this.state = seed;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive, was " + bound);
        }
        // Rejection sampling, identical to Random.nextInt(int), to avoid modulo bias.
        int r = nextBits(31);
        int m = bound - 1;
        if ((bound & m) == 0) {            // bound is a power of two
            r = (int) ((bound * (long) r) >> 31);
        } else {
            for (int x = r; x - (r = x % bound) + m < 0; x = nextBits(31)) {
                // loop until an unbiased value is drawn
            }
        }
        return r;
    }

    private int nextBits(int bits) {
        long z = (state += 0x9E37_79B9_7F4A_7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D0_49BB_1331_11EBL;
        z = z ^ (z >>> 31);
        return (int) (z >>> (64 - bits));
    }

    @Override
    public long getState() {
        return state;
    }

    @Override
    public void setState(long state) {
        this.state = state;
    }
}
