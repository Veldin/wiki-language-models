package com.veldin.finalmodels;

import com.veldin.ngrampackstrategy.NGramPackStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FinalNGramModel {

    private final int[] keys;
    private final short[] values;
    private final byte[] used;
    private final NGramPackStrategy strategy;
    private final int mask;

    public FinalNGramModel(
            int[] keys,
            short[] values,
            byte[] used,
            NGramPackStrategy strategy
    ) {
        if (keys == null
                || values == null
                || used == null
                || strategy == null) {
            throw new NullPointerException();
        }

        if (keys.length != values.length
                || keys.length != used.length) {
            throw new IllegalArgumentException(
                    "Keys, values and used must have the same length"
            );
        }

        if (keys.length == 0
                || (keys.length & (keys.length - 1)) != 0) {
            throw new IllegalArgumentException(
                    "Table size must be a non-zero power of two"
            );
        }

        this.keys = keys;
        this.values = values;
        this.used = used;
        this.strategy = strategy;
        this.mask = keys.length - 1;
    }

    /**
     * Return the best prediction for the supplied context.
     *
     * @return prediction 0-254, or -1 when no prediction exists
     */
    public int predict(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {
        int key = strategy.pack(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        );

        int index = find(key);

        if (index < 0) {
            return -1;
        }

        return values[index] & 0xFF;
    }

    /**
     * Return all predictions for the supplied context.
     *
     * @return zero, one or two predictions
     */
    public List<Integer> predictAll(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {
        int key = strategy.pack(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        );

        int index = find(key);

        if (index < 0) {
            return List.of();
        }

        int packed = values[index] & 0xFFFF;

        int first = packed & 0xFF;
        int second = (packed >>> 8) & 0xFF;

        if (second == 255) {
            return List.of(first);
        }

        if (first == second) {
            return List.of(first);
        }

        List<Integer> result = new ArrayList<>(2);
        result.add(first);
        result.add(second);

        return result;
    }

    /**
     * Find the table slot containing the supplied key.
     *
     * @return array index, or -1 when the key does not exist
     */
    private int find(int key) {
        int index = mix(key) & mask;

        while (used[index] != 0) {
            if (keys[index] == key) {
                return index;
            }

            index = (index + 1) & mask;
        }

        return -1;
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return value;
    }

    public int size() {
        int count = 0;

        for (byte value : used) {
            if (value != 0) {
                count++;
            }
        }

        return count;
    }

    public int[] getKeys() {
        return keys;
    }

    public short[] getValues() {
        return values;
    }

    public byte[] getUsed() {
        return used;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keys);
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(used);
        result = 31 * result + strategy.hashCode();
        return result;
    }
}