package com.veldin.builders;


import com.veldin.finalmodels.FinalNGramModel;
import com.veldin.ngrampackstrategy.NGramPackStrategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FinalNGramBuilder {

    private static final int HEADER_SIZE = 8;
    private static final int ENTRY_SIZE = 6;

    private FinalNGramBuilder() {
    }

    public static FinalNGramModel load(
            Path file,
            NGramPackStrategy strategy
    ) throws IOException {

        long fileSize = Files.size(file);

        if (fileSize < HEADER_SIZE) {
            throw new IOException(
                    "N-gram file is too small: " + fileSize
            );
        }

        long dataSize = fileSize - HEADER_SIZE;

        if (dataSize % ENTRY_SIZE != 0) {
            throw new IOException(
                    "Invalid n-gram file size: " + fileSize
            );
        }

        int entries = Math.toIntExact(
                dataSize / ENTRY_SIZE
        );

        int capacity = tableSizeFor(entries);

        int[] keys = new int[capacity];
        short[] values = new short[capacity];
        byte[] used = new byte[capacity];

        try (DataInputStream in =
                     new DataInputStream(
                             new BufferedInputStream(
                                     Files.newInputStream(file),
                                     1024 * 1024
                             ))) {

            int magic = in.readInt();

            if (magic != ModelFileFormatStatics.MAGIC) {
                throw new IOException(
                        "Invalid model file. "
                                + "Expected magic 0x"
                                + Integer.toHexString(
                                ModelFileFormatStatics.MAGIC
                        )
                                + ", got 0x"
                                + Integer.toHexString(magic)
                );
            }

            int type = in.readInt();

            if (type != ModelFileFormatStatics.TYPE_NGRAM) {
                throw new IOException(
                        "Invalid model file type. "
                                + "Expected n-gram ("
                                + ModelFileFormatStatics.TYPE_NGRAM
                                + "), got "
                                + type
                );
            }

            for (int i = 0; i < entries; i++) {
                int key = in.readInt();
                short value = in.readShort();

                put(
                        keys,
                        values,
                        used,
                        key,
                        value
                );
            }
        }

        return new FinalNGramModel(
                keys,
                values,
                used,
                strategy
        );
    }

    public static void save(
            Path file,
            FinalNGramModel model
    ) throws IOException {

        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (DataOutputStream out =
                     new DataOutputStream(
                             new BufferedOutputStream(
                                     Files.newOutputStream(file),
                                     1024 * 1024
                             ))) {

            out.writeInt(
                    ModelFileFormatStatics.MAGIC
            );

            out.writeInt(
                    ModelFileFormatStatics.TYPE_NGRAM
            );

            int[] keys = model.getKeys();
            short[] values = model.getValues();
            byte[] used = model.getUsed();

            for (int i = 0; i < keys.length; i++) {

                if (used[i] == 0) {
                    continue;
                }

                out.writeInt(keys[i]);
                out.writeShort(values[i]);
            }
        }
    }

    private static void put(
            int[] keys,
            short[] values,
            byte[] used,
            int key,
            short value
    ) {
        int mask = keys.length - 1;
        int index = mix(key) & mask;

        while (used[index] != 0) {
            if (keys[index] == key) {
                values[index] = value;
                return;
            }

            index = (index + 1) & mask;
        }

        keys[index] = key;
        values[index] = value;
        used[index] = 1;
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return value;
    }

    private static int tableSizeFor(int entries)
            throws IOException {

        if (entries < 1) {
            return 2;
        }

        /*
         * Keep load factor <= 75%.
         */
        long required =
                ((long) entries * 4L + 2L) / 3L;

        if (required > (1L << 30)) {
            throw new IOException(
                    "N-gram model is too large: "
                            + entries
                            + " entries"
            );
        }

        int capacity = 1;

        while (capacity < required) {
            capacity <<= 1;
        }

        return capacity;
    }
}