package ru.anseranser.eval;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A tiny ONNX model for the tests: it reads the vector of the seat and scores
 * every action.
 *
 * <p>The model is written as an ONNX protobuf by hand, the way the tests of
 * the {@code ml} module write theirs. The graph is {@code Slice(vector) ->
 * scores}, so the score of an action is one of the values of the vector. The
 * model is a bad player and a legal one: it always chooses one of the legal
 * moves, which is what the evaluator has to survive.
 */
final class TinyModel {

    private static final int FLOAT = 1;
    private static final int INT64 = 7;
    private static final int IR_VERSION = 8;
    private static final int OPSET = 13;

    private TinyModel() {
    }

    /** A model of the expected shape, written into {@code directory}. */
    static Path write(Path directory, int inputs, int actions) throws IOException {
        Path file = Files.createTempFile(directory, "tiny-", ".onnx");
        Files.write(file, model(inputs, actions));
        return file;
    }

    private static byte[] model(int inputs, int actions) {
        byte[] graph = graph(inputs, actions);
        byte[] opset = concat(stringField(1, ""), varintField(2, OPSET));
        return concat(varintField(1, IR_VERSION), stringField(2, "trumpcards"),
                bytesField(7, graph), bytesField(8, opset));
    }

    private static byte[] graph(int inputs, int actions) {
        byte[] slice = node("Slice", List.of("vector", "starts", "ends", "axes"), List.of("scores"));
        return concat(bytesField(1, slice),
                stringField(2, "tiny"),
                bytesField(5, intTensor("starts", new long[]{0})),
                bytesField(5, intTensor("ends", new long[]{actions})),
                bytesField(5, intTensor("axes", new long[]{1})),
                bytesField(11, valueInfo("vector", FLOAT, 1, inputs)),
                bytesField(12, valueInfo("scores", FLOAT, 1, actions)));
    }

    private static byte[] intTensor(String name, long[] values) {
        ByteBuffer data = ByteBuffer.allocate(values.length * Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (long value : values) {
            data.putLong(value);
        }
        return concat(varintField(1, 1), varintField(2, INT64), stringField(8, name),
                bytesField(9, data.array()));
    }

    private static byte[] valueInfo(String name, int elemType, int... dims) {
        byte[][] dimensions = new byte[dims.length][];
        for (int i = 0; i < dims.length; i++) {
            dimensions[i] = bytesField(1, varintField(1, dims[i]));
        }
        byte[] shape = concat(dimensions);
        byte[] tensorType = concat(varintField(1, elemType), bytesField(2, shape));
        return concat(stringField(1, name), bytesField(2, bytesField(1, tensorType)));
    }

    private static byte[] node(String opType, List<String> inputs, List<String> outputs) {
        byte[][] parts = new byte[inputs.size() + outputs.size() + 1][];
        int i = 0;
        for (String input : inputs) {
            parts[i++] = stringField(1, input);
        }
        for (String output : outputs) {
            parts[i++] = stringField(2, output);
        }
        parts[i] = stringField(4, opType);
        return concat(parts);
    }

    private static byte[] varintField(int field, long value) {
        return concat(varint(((long) field << 3)), varint(value));
    }

    private static byte[] bytesField(int field, byte[] payload) {
        return concat(varint(((long) field << 3) | 2), varint(payload.length), payload);
    }

    private static byte[] stringField(int field, String value) {
        return bytesField(field, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((value & ~0x7FL) != 0) {
            out.write((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write((int) value);
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
