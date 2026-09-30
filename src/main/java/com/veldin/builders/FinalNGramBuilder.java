package com.veldin.builders;

import com.veldin.finalmodels.FinalNGramModel;
import com.veldin.ngrampackstrategy.NGramPackStrategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class FinalNGramBuilder {

    private static final int HEADER_SIZE = 8;

    private FinalNGramBuilder() {
    }

    /*
     * ============================================================
     * SAVE
     * ============================================================
     *
     * Header:
     *
     * 4 bytes = VLM1 magic
     * 4 bytes = TYPE_NGRAM
     *
     * Each entry:
     *
     * 4 bytes = context Integer
     * 2 bytes = predictions Short
     */

    public static void save(
            Path file,
            FinalNGramModel learner
    ) throws IOException {

        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (DataOutputStream out =
                     new DataOutputStream(
                             new BufferedOutputStream(
                                     Files.newOutputStream(file),
                                     1024 * 1024))) {

            // Common model file header
            out.writeInt(
                    ModelFileFormatStatics.MAGIC
            );

            out.writeInt(
                    ModelFileFormatStatics.TYPE_NGRAM
            );

            // N-gram entries
            for (var entry :
                    learner.getPredictions().entrySet()) {

                out.writeInt(
                        entry.getKey()
                );

                out.writeShort(
                        entry.getValue()
                );
            }
        }
    }

    public static FinalNGramModel load(
            Path file,
            NGramPackStrategy strategy
    ) throws IOException {

        long fileSize =
                Files.size(file);

        if (fileSize < HEADER_SIZE) {
            throw new IOException(
                    "N-gram file is too small: "
                            + fileSize
            );
        }

        long dataSize =
                fileSize - HEADER_SIZE;

        if (dataSize % 6 != 0) {
            throw new IOException(
                    "Invalid n-gram file size: "
                            + fileSize
            );
        }

        int entries =
                Math.toIntExact(
                        dataSize / 6
                );

        Map<Integer, Short> predictions =
                new HashMap<>(
                        (int) (entries / 0.75f) + 1
                );

        try (DataInputStream in =
                     new DataInputStream(
                             new BufferedInputStream(
                                     Files.newInputStream(file),
                                     1024 * 1024))) {

            // ----- common model file header -----

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

            // ----- n-gram entries -----

            for (int i = 0;
                 i < entries;
                 i++) {

                int key =
                        in.readInt();

                short value =
                        in.readShort();

                predictions.put(
                        key,
                        value
                );
            }
        }

        return new FinalNGramModel(
                predictions,
                strategy
        );
    }
}