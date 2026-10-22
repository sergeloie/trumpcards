package ru.anseranser.eval;

/**
 * A broken invariant: the seed and the step that broke it, so that the tool
 * can say which game went wrong and where.
 */
final class InvariantViolation extends RuntimeException {

    InvariantViolation(long seed, int step, String detail) {
        super("seed " + seed + ", step " + step + ": " + detail);
    }

    InvariantViolation(long seed, int step, String detail, Throwable cause) {
        super("seed " + seed + ", step " + step + ": " + detail, cause);
    }
}
