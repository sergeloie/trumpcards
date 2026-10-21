package ru.anseranser.engine;

/**
 * The random generator of the engine, ported verbatim from the old engine's
 * {@code SplitMix64}: the same constants and the same rejection sampling.
 *
 * <p>The whole position of the generator is its state, so a state carries it
 * and a game can be continued from any state.
 */
final class SplitMix64 {

    private long state;

    SplitMix64(long state) {
        this.state = state;
    }

    int nextInt(int bound) {
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

    long state() {
        return state;
    }
}
