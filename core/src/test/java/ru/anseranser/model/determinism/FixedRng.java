package ru.anseranser.model.determinism;

import ru.anseranser.model.Rng;

/**
 * A fully predetermined {@link Rng} for golden scenarios. With {@code value == 0}
 * it makes {@code nextInt(n)} return {@code 0}, which pins {@code Game}'s random
 * first-dealer choice to the first seat — so a golden test knows exactly who
 * leads without re-implementing {@link ru.anseranser.model.SplitMix64}.
 *
 * <p>Combine with {@link ru.anseranser.model.ScriptedCardDealer} to remove the
 * last source of randomness from a scenario.</p>
 */
final class FixedRng implements Rng {

    private long state;

    FixedRng(long value) {
        this.state = value;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive, was " + bound);
        }
        return (int) Math.floorMod(state, bound);
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
