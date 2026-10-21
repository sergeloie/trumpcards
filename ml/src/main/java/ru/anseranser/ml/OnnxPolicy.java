package ru.anseranser.ml;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.State;

import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A learned policy: a model that scores every action of the game, and the
 * legal move the best score points at.
 *
 * <p>The model is read as it was written: one input of {@link Vector#LENGTH}
 * floats and one output of {@link Vector#ACTIONS} scores. The names of the
 * input and of the output are taken from the model itself.
 *
 * <p>The session runs on one thread. Multi-threaded inference is not
 * bit-reproducible: a different order of the reductions gives slightly
 * different logits, and a borderline maximum can then choose another card.
 *
 * <p>Nothing is guessed: a model that is not there, a model that does not fit
 * the contract and an action that is not among the legal moves all stop the
 * game instead of quietly choosing something.
 */
public final class OnnxPolicy implements AutoCloseable {

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;
    private final long[] inputShape;

    /**
     * A policy that plays the model at {@code model}.
     *
     * @throws IllegalArgumentException when there is no model at that path
     * @throws IllegalStateException when the model does not fit the contract
     */
    public OnnxPolicy(Path model) {
        Path path = model.toAbsolutePath();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("the model is not there: " + path);
        }
        this.environment = OrtEnvironment.getEnvironment();
        Loaded loaded = load(environment, path);
        this.session = loaded.session();
        this.inputName = loaded.inputName();
        this.outputName = loaded.outputName();
        this.inputShape = loaded.inputShape();
    }

    /**
     * The move the model chooses: the index in the list of legal moves.
     *
     * @throws IllegalArgumentException when the game is over and there is no move to choose
     * @throws IllegalStateException when the model scores an action that is not legal
     */
    public int choose(State state, List<Move> moves) {
        if (moves.isEmpty()) {
            throw new IllegalArgumentException("the game is over, there is no move to choose");
        }
        Vector vector = Vector.of(state, state.turn(), moves);
        int action = best(score(vector.values()), vector.mask());
        return indexOf(moves, action);
    }

    /**
     * Closes the session. The environment is shared by every session and stays
     * open.
     */
    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new IllegalStateException("the model could not be closed", e);
        }
    }

    private static Loaded load(OrtEnvironment environment, Path path) {
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(1);
            options.setInterOpNumThreads(1);
            OrtSession session = environment.createSession(path.toString(), options);
            Map<String, NodeInfo> inputs = session.getInputInfo();
            Map<String, NodeInfo> outputs = session.getOutputInfo();
            String inputName = onlyName(inputs, "input");
            String outputName = onlyName(outputs, "output");
            long[] inputShape = fedShape(shape(inputs.get(inputName), Vector.LENGTH, "input", path));
            shape(outputs.get(outputName), Vector.ACTIONS, "output", path);
            return new Loaded(session, inputName, outputName, inputShape);
        } catch (OrtException e) {
            throw new IllegalStateException("the model could not be loaded: " + path, e);
        }
    }

    /** The name of the only input or output the model has. */
    private static String onlyName(Map<String, NodeInfo> infos, String kind) {
        if (infos.size() != 1) {
            throw new IllegalStateException("the model has " + infos.size() + " " + kind
                    + "s, expected one");
        }
        return infos.keySet().iterator().next();
    }

    /** The shape the model declares, when it holds as many values as expected. */
    private static long[] shape(NodeInfo info, int values, String kind, Path path) {
        if (!(info.getInfo() instanceof TensorInfo tensor)) {
            throw new IllegalStateException("the model at " + path + " does not use a tensor");
        }
        long[] shape = tensor.getShape();
        long count = 1;
        for (long dim : shape) {
            if (dim > 0) {
                count *= dim;
            }
        }
        if (count != values) {
            throw new IllegalStateException("the model at " + path + " has " + count
                    + " values in its " + kind + ", expected " + values);
        }
        return shape;
    }

    /** The shape the vector is fed in: a batch of one, or a plain vector. */
    private static long[] fedShape(long[] declared) {
        return declared.length == 1 ? new long[]{Vector.LENGTH} : new long[]{1, Vector.LENGTH};
    }

    /** The score of every action the model gives. */
    private float[] score(float[] values) {
        try (OnnxTensor input = OnnxTensor.createTensor(environment, FloatBuffer.wrap(values), inputShape);
             OrtSession.Result result = session.run(Map.of(inputName, input))) {
            OnnxValue output = result.get(outputName)
                    .orElseThrow(() -> new IllegalStateException("the model has no output named " + outputName));
            if (!(output instanceof OnnxTensor tensor)) {
                throw new IllegalStateException("the model does not score with a tensor");
            }
            FloatBuffer buffer = tensor.getFloatBuffer();
            float[] scores = new float[buffer.remaining()];
            buffer.get(scores);
            if (scores.length != Vector.ACTIONS) {
                throw new IllegalStateException("the model scored " + scores.length
                        + " actions, expected " + Vector.ACTIONS);
            }
            return scores;
        } catch (OrtException e) {
            throw new IllegalStateException("the model could not be run", e);
        }
    }

    /** The action the mask allows with the highest score. */
    private static int best(float[] scores, long mask) {
        int best = -1;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int action = 0; action < Vector.ACTIONS; action++) {
            if ((mask & (1L << action)) == 0) {
                continue;
            }
            if (scores[action] > bestScore) {
                bestScore = scores[action];
                best = action;
            }
        }
        if (best < 0) {
            throw new IllegalStateException("the model scored no legal action");
        }
        return best;
    }

    /** The legal move that plays the action. */
    private static int indexOf(List<Move> moves, int action) {
        for (int index = 0; index < moves.size(); index++) {
            Move move = moves.get(index);
            boolean chosen = action == Vector.TAKE_POT
                    ? move instanceof Move.TakePot
                    : move instanceof Move.Play play && Vector.actionOf(play.card()) == action;
            if (chosen) {
                return index;
            }
        }
        throw new IllegalStateException("the model chose the action " + action
                + ", which is not among the legal moves");
    }

    /** What the model gave the policy: the session and the names it carries. */
    private record Loaded(OrtSession session, String inputName, String outputName, long[] inputShape) {
    }
}
