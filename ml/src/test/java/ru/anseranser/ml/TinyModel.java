package ru.anseranser.ml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny ONNX model for the tests: it reads the vector of the seat and scores
 * every action.
 *
 * <p>The model is written as an ONNX protobuf by hand. The graph is
 * {@code Slice(vector) -> Mul(gain) -> Add(bias)}, so the output is the first
 * {@link Vector#ACTIONS} features of the vector, scaled and shifted. A model
 * that always favours one action is a gain of zero and a bias with a single
 * one in it; a model that reads the vector is a gain of ones and a bias of
 * zeros.
 */
final class TinyModel {

    private static final int FLOAT = 1;
    private static final int INT64 = 7;
    private static final int IR_VERSION = 8;
    private static final int OPSET = 13;

    private TinyModel() {
    }

    /** A model of the expected shape, written into {@code directory}. */
    static Path write(Path directory, float[] gain, float[] bias) throws IOException {
        return write(directory, Vector.LENGTH, Vector.ACTIONS, gain, bias);
    }

    /** A model of the given shape, written into {@code directory}. */
    static Path write(Path directory, int inputs, int actions, float[] gain, float[] bias) throws IOException {
        Path file = Files.createTempFile(directory, "tiny-", ".onnx");
        Files.write(file, model(inputs, actions, gain, bias));
        return file;
    }

    private static byte[] model(int inputs, int actions, float[] gain, float[] bias) {
        byte[] graph = graph(inputs, actions, gain, bias);
        byte[] opset = concat(stringField(1, ""), varintField(2, OPSET));
        return concat(varintField(1, IR_VERSION), stringField(2, "trumpcards"),
                bytesField(7, graph), bytesField(8, opset));
    }

    private static byte[] graph(int inputs, int actions, float[] gain, float[] bias) {
        byte[] slice = node("Slice", List.of("vector", "starts", "ends", "axes"), List.of("features"));
        byte[] multiply = node("Mul", List.of("features", "gain"), List.of("scaled"));
        byte[] add = node("Add", List.of("scaled", "bias"), List.of("scores"));
        return concat(bytesField(1, slice), bytesField(1, multiply), bytesField(1, add),
                stringField(2, "tiny"),
                bytesField(5, intTensor("starts", new long[]{0}, 1)),
                bytesField(5, intTensor("ends", new long[]{actions}, 1)),
                bytesField(5, intTensor("axes", new long[]{1}, 1)),
                bytesField(5, tensor("gain", gain, 1, actions)),
                bytesField(5, tensor("bias", bias, 1, actions)),
                bytesField(11, valueInfo("vector", FLOAT, 1, inputs)),
                bytesField(12, valueInfo("scores", FLOAT, 1, actions)));
    }

    private static byte[] intTensor(String name, long[] values, int... dims) {
        ByteBuffer data = ByteBuffer.allocate(values.length * Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (long value : values) {
            data.putLong(value);
        }
        List<byte[]> parts = new ArrayList<>();
        for (int dim : dims) {
            parts.add(varintField(1, dim));
        }
        parts.add(varintField(2, INT64));
        parts.add(stringField(8, name));
        parts.add(bytesField(9, data.array()));
        return concat(parts.toArray(new byte[0][]));
    }

    private static byte[] tensor(String name, float[] values, int... dims) {
        ByteBuffer data = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) {
            data.putFloat(value);
        }
        List<byte[]> parts = new ArrayList<>();
        for (int dim : dims) {
            parts.add(varintField(1, dim));
        }
        parts.add(varintField(2, FLOAT));
        parts.add(stringField(8, name));
        parts.add(bytesField(9, data.array()));
        return concat(parts.toArray(new byte[0][]));
    }

    private static byte[] valueInfo(String name, int elemType, long... dims) {
        List<byte[]> dimensions = new ArrayList<>();
        for (long dim : dims) {
            dimensions.add(bytesField(1, varintField(1, dim)));
        }
        byte[] shape = concat(dimensions.toArray(new byte[0][]));
        byte[] tensorType = concat(varintField(1, elemType), bytesField(2, shape));
        return concat(stringField(1, name), bytesField(2, bytesField(1, tensorType)));
    }

    private static byte[] node(String opType, List<String> inputs, List<String> outputs, byte[]... attributes) {
        List<byte[]> parts = new ArrayList<>();
        for (String input : inputs) {
            parts.add(stringField(1, input));
        }
        for (String output : outputs) {
            parts.add(stringField(2, output));
        }
        parts.add(stringField(4, opType));
        for (byte[] attribute : attributes) {
            parts.add(bytesField(5, attribute));
        }
        return concat(parts.toArray(new byte[0][]));
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
